-- ============================================================
-- Calendux / Agendux - Revised V1 (PostgreSQL + Flyway)
-- Goals:
-- 1) timestamptz for absolute instants (appointments / expires)
-- 2) event types (like Calendly) per profile
-- 3) prevent overlapping appointments at DB level (EXCLUDE)
-- 4) prevent overlapping availability rules / overrides at DB level
-- 5) safer invite tokens (store token hash)
-- ============================================================

-- Needed for EXCLUDE constraints combining equality on ints with range overlap
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ----------------------------
-- Users (base authentication table)
-- -----------------------
       -- -----
CREATE TABLE tb_users (
                          id         BIGSERIAL PRIMARY KEY,
                          email      VARCHAR(255) NOT NULL UNIQUE,
                          full_name  VARCHAR(160),
                          created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ----------------------------
-- Profiles (1 per user)
-- ----------------------------
CREATE TABLE tb_profiles (
                             id              BIGSERIAL PRIMARY KEY,
                             user_id          BIGINT NOT NULL UNIQUE,
                             display_name     VARCHAR(120) NOT NULL,
                             timezone         VARCHAR(60)  NOT NULL DEFAULT 'UTC',
    -- public slug for the profile page (e.g. /sebastian)
                             profile_slug     VARCHAR(120) NOT NULL UNIQUE,
                             created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             CONSTRAINT fk_profiles_user
                                 FOREIGN KEY (user_id) REFERENCES tb_users(id)
                                     ON DELETE CASCADE
);

-- ----------------------------
-- Event types / Scheduling pages (like Calendly event types)
-- e.g. /{profile_slug}/{event_slug}
-- ----------------------------
CREATE TABLE tb_event_types (
                                id                    BIGSERIAL PRIMARY KEY,
                                profile_id            BIGINT NOT NULL,
                                title                 VARCHAR(120) NOT NULL,
                                event_slug            VARCHAR(120) NOT NULL,
                                duration_minutes      INT NOT NULL,
                                buffer_before_minutes INT NOT NULL DEFAULT 0,
                                buffer_after_minutes  INT NOT NULL DEFAULT 0,
                                is_active             BOOLEAN NOT NULL DEFAULT TRUE,
                                created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                                updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                                CONSTRAINT fk_event_types_profile
                                    FOREIGN KEY (profile_id) REFERENCES tb_profiles(id)
                                        ON DELETE CASCADE,
                                CONSTRAINT ck_event_types_duration CHECK (duration_minutes > 0 AND duration_minutes <= 24*60),
                                CONSTRAINT ck_event_types_buffers  CHECK (buffer_before_minutes >= 0 AND buffer_after_minutes >= 0),
                                CONSTRAINT uq_event_types_profile_slug UNIQUE (profile_id, event_slug)
);

-- ----------------------------
-- Weekly availability rules
-- Stored as local time windows (time-of-day) per weekday
-- Prevent overlaps per (profile, day_of_week) at DB level using a tsrange on a fixed date
-- ----------------------------
CREATE TABLE tb_availability_rules (
                                       id           BIGSERIAL PRIMARY KEY,
                                       profile_id   BIGINT NOT NULL,
                                       day_of_week  SMALLINT NOT NULL,     -- 0..6 (Sunday..Saturday) or (Mon..Sun) depending on your app convention
                                       start_time   TIME NOT NULL,
                                       end_time     TIME NOT NULL,
                                       is_available BOOLEAN NOT NULL DEFAULT TRUE,
                                       created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- range computed on an arbitrary fixed date for overlap checks
                                       rule_range   TSRANGE GENERATED ALWAYS AS (
                                           tsrange(
                                                   ('2000-01-01'::date + start_time),
                                                   ('2000-01-01'::date + end_time)
                                                   + CASE WHEN end_time <= start_time THEN INTERVAL '1 day' ELSE INTERVAL '0' END,
                                                   '[)'
                                           )
                                           ) STORED,

                                       CONSTRAINT fk_rules_profile
                                           FOREIGN KEY (profile_id) REFERENCES tb_profiles(id)
                                               ON DELETE CASCADE,
                                       CONSTRAINT ck_rules_day_of_week CHECK (day_of_week BETWEEN 0 AND 6),
                                       CONSTRAINT ck_rules_time_range  CHECK (end_time <> start_time)
);

-- No overlapping availability windows for same profile/day (regardless of is_available)
ALTER TABLE tb_availability_rules
    ADD CONSTRAINT ex_rules_no_overlap
    EXCLUDE USING gist (
    profile_id WITH =,
    day_of_week WITH =,
    rule_range WITH &&
);

CREATE INDEX idx_rules_profile_day ON tb_availability_rules(profile_id, day_of_week);

-- ----------------------------
-- Date-specific overrides (block or open on a specific date)
-- Stored as local date + local time window
-- Prevent overlaps per (profile, override_date)
-- ----------------------------
CREATE TABLE tb_availability_overrides (
                                           id            BIGSERIAL PRIMARY KEY,
                                           profile_id    BIGINT NOT NULL,
                                           override_date DATE NOT NULL,
                                           start_time    TIME NOT NULL,
                                           end_time      TIME NOT NULL,
                                           is_available  BOOLEAN NOT NULL DEFAULT TRUE,
                                           created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                           updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

                                           override_range TSRANGE GENERATED ALWAYS AS (
                                               tsrange(
                                                       (override_date + start_time),
                                                       (override_date + end_time)
                                                       + CASE WHEN end_time <= start_time THEN INTERVAL '1 day' ELSE INTERVAL '0' END,
                                                       '[)'
                                               )
                                               ) STORED,

                                           CONSTRAINT fk_overrides_profile
                                               FOREIGN KEY (profile_id) REFERENCES tb_profiles(id)
                                                   ON DELETE CASCADE,
                                           CONSTRAINT ck_overrides_time_range CHECK (end_time <> start_time)
);

ALTER TABLE tb_availability_overrides
    ADD CONSTRAINT ex_overrides_no_overlap
    EXCLUDE USING gist (
    profile_id WITH =,
    override_date WITH =,
    override_range WITH &&
);

CREATE INDEX idx_overrides_profile_date ON tb_availability_overrides(profile_id, override_date);

-- ----------------------------
-- Shareable invite links (for guests)
-- Store token HASH (recommended), not the raw token
-- token_hash example: sha256 hex => 64 chars
-- ----------------------------
CREATE TABLE tb_invite_links (
                                 id           BIGSERIAL PRIMARY KEY,
                                 event_type_id BIGINT NOT NULL,
                                 token_hash   CHAR(64) NOT NULL UNIQUE,
                                 expires_at   TIMESTAMPTZ,
                                 max_bookings INT,
                                 is_active    BOOLEAN NOT NULL DEFAULT TRUE,
                                 created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT fk_invite_event_type
                                     FOREIGN KEY (event_type_id) REFERENCES tb_event_types(id)
                                         ON DELETE CASCADE,
                                 CONSTRAINT ck_invite_max_bookings CHECK (max_bookings IS NULL OR max_bookings > 0)
);

CREATE INDEX idx_invite_event_active ON tb_invite_links(event_type_id, is_active);

-- ----------------------------
-- Guests (optional profiles for unregistered invitees)
-- ----------------------------
CREATE TABLE tb_guests (
                           id         BIGSERIAL PRIMARY KEY,
                           full_name  VARCHAR(120) NOT NULL,
                           email      VARCHAR(255) NOT NULL,
                           created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           CONSTRAINT uq_guests_email UNIQUE (email)
);

CREATE INDEX idx_guests_email ON tb_guests(email);

-- ----------------------------
-- Appointments
-- Store absolute instants as timestamptz (UTC recommended)
-- Prevent overlapping "blocking" appointments per profile at DB level
-- ----------------------------
CREATE TABLE tb_appointments (
                                 id            BIGSERIAL PRIMARY KEY,
                                 profile_id    BIGINT NOT NULL,
                                 event_type_id BIGINT NOT NULL,
                                 invite_link_id BIGINT,
                                 guest_id      BIGINT NOT NULL,
                                 starts_at     TIMESTAMPTZ NOT NULL,
                                 ends_at       TIMESTAMPTZ NOT NULL,
                                 status        VARCHAR(30) NOT NULL DEFAULT 'BOOKED',
                                 notes         TEXT,
                                 created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

                                 appt_range    TSTZRANGE GENERATED ALWAYS AS (
                                     tstzrange(starts_at, ends_at, '[)')
                                     ) STORED,

                                 CONSTRAINT fk_appointments_profile
                                     FOREIGN KEY (profile_id) REFERENCES tb_profiles(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_appointments_event_type
                                     FOREIGN KEY (event_type_id) REFERENCES tb_event_types(id)
                                         ON DELETE RESTRICT,

                                 CONSTRAINT fk_appointments_invite_link
                                     FOREIGN KEY (invite_link_id) REFERENCES tb_invite_links(id)
                                         ON DELETE SET NULL,

                                 CONSTRAINT fk_appointments_guest
                                     FOREIGN KEY (guest_id) REFERENCES tb_guests(id)
                                         ON DELETE RESTRICT,

                                 CONSTRAINT ck_appointments_time_range CHECK (ends_at > starts_at),
                                 CONSTRAINT ck_appointments_status CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED', 'RESCHEDULED'))
);

-- Prevent double-booking for blocking statuses (BOOKED/COMPLETED)
-- NOTE: This relies on PostgreSQL supporting WHERE on EXCLUDE constraints (common in modern PG).
ALTER TABLE tb_appointments
    ADD CONSTRAINT ex_appointments_no_overlap
    EXCLUDE USING gist (
    profile_id WITH =,
    appt_range WITH &&
)
WHERE (status IN ('BOOKED', 'COMPLETED'));

CREATE INDEX idx_appointments_profile_time ON tb_appointments(profile_id, starts_at, ends_at);
CREATE INDEX idx_appointments_event_time   ON tb_appointments(event_type_id, starts_at);
CREATE INDEX idx_appointments_invite_status ON tb_appointments(invite_link_id, status);

-- ----------------------------
-- Enforce max bookings on invite links
-- ----------------------------
CREATE OR REPLACE FUNCTION enforce_invite_max_bookings()
RETURNS TRIGGER AS $$
DECLARE
    max_allowed INT;
    current_count INT;
BEGIN
    IF NEW.invite_link_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT max_bookings INTO max_allowed
    FROM tb_invite_links
    WHERE id = NEW.invite_link_id;

    IF max_allowed IS NULL THEN
        RETURN NEW;
    END IF;

    IF NEW.status NOT IN ('BOOKED', 'COMPLETED') THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO current_count
    FROM tb_appointments
    WHERE invite_link_id = NEW.invite_link_id
      AND status IN ('BOOKED', 'COMPLETED')
      AND (TG_OP = 'INSERT' OR id <> NEW.id);

    IF current_count >= max_allowed THEN
        RAISE EXCEPTION 'Invite link % has reached its maximum bookings (%).', NEW.invite_link_id, max_allowed;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_invite_max_bookings
    BEFORE INSERT OR UPDATE OF invite_link_id, status ON tb_appointments
    FOR EACH ROW
    EXECUTE FUNCTION enforce_invite_max_bookings();

-- ----------------------------
-- Generic function to refresh updated_at on row updates
-- ----------------------------
CREATE OR REPLACE FUNCTION update_updated_at_()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------
-- updated_at triggers using update_updated_at_()
-- ----------------------------
CREATE TRIGGER trg_update_users_updated_at
    BEFORE UPDATE ON tb_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_profiles_updated_at
    BEFORE UPDATE ON tb_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_event_types_updated_at
    BEFORE UPDATE ON tb_event_types
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_rules_updated_at
    BEFORE UPDATE ON tb_availability_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_overrides_updated_at
    BEFORE UPDATE ON tb_availability_overrides
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_invite_links_updated_at
    BEFORE UPDATE ON tb_invite_links
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_guests_updated_at
    BEFORE UPDATE ON tb_guests
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();

CREATE TRIGGER trg_update_appointments_updated_at
    BEFORE UPDATE ON tb_appointments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_();
