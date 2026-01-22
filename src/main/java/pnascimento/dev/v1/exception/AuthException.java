package pnascimento.dev.v1.exception;

import org.springframework.http.HttpStatus;

public class AuthException {
    
    public static class EmailAlreadyExists extends AppException {
        public EmailAlreadyExists() {
            super("Este e-mail já está cadastrado no sistema.", HttpStatus.BAD_REQUEST);
        }
    }

    public static class InvalidCredentials extends AppException {
        public InvalidCredentials() {
            super("E-mail ou senha incorretos.", HttpStatus.UNAUTHORIZED);
        }
    }

    public static class AccountDisabled extends AppException {
        public AccountDisabled() {
            super("Sua conta está desativada. Entre em contato com o suporte.", HttpStatus.FORBIDDEN);
        }
    }
}
