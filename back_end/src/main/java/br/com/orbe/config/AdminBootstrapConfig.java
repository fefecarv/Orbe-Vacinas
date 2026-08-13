package br.com.orbe.config;

import br.com.orbe.exception.BusinessException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AdminBootstrapConfig {

    private final boolean enabled;
    private final String name;
    private final String cpf;
    private final String email;
    private final String password;
    private final String phone;
    private final LocalDate birthDate;

    private AdminBootstrapConfig(
            boolean enabled,
            String name,
            String cpf,
            String email,
            String password,
            String phone,
            LocalDate birthDate
    ) {
        this.enabled = enabled;
        this.name = name;
        this.cpf = cpf;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    public static AdminBootstrapConfig fromEnvironment() {
        boolean enabled = Boolean.parseBoolean(System.getenv(
                "ORBE_BOOTSTRAP_ADMIN_ENABLED"
        ));
        if (!enabled) {
            return new AdminBootstrapConfig(false, null, null, null, null, null, null);
        }

        String name = required("ORBE_BOOTSTRAP_ADMIN_NAME");
        String cpf = digits(required("ORBE_BOOTSTRAP_ADMIN_CPF"));
        String email = required("ORBE_BOOTSTRAP_ADMIN_EMAIL").trim().toLowerCase();
        String password = required("ORBE_BOOTSTRAP_ADMIN_PASSWORD");
        String phone = required("ORBE_BOOTSTRAP_ADMIN_PHONE");
        String rawBirthDate = required("ORBE_BOOTSTRAP_ADMIN_BIRTH_DATE");
        try {
            return new AdminBootstrapConfig(
                    true,
                    name,
                    cpf,
                    email,
                    password,
                    phone,
                    LocalDate.parse(rawBirthDate)
            );
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    "ORBE_BOOTSTRAP_ADMIN_BIRTH_DATE deve usar o formato AAAA-MM-DD."
            );
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new BusinessException("A variavel " + name + " e obrigatoria.");
        }
        return value;
    }

    private static String digits(String value) {
        return value.replaceAll("\\D", "");
    }

    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public LocalDate getBirthDate() { return birthDate; }
}
