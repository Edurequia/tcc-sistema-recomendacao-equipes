package br.ufn.tfg.teamrec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: garante que o contexto do Spring sobe sem erro de configuracao. */
@SpringBootTest
@ActiveProfiles("dev")
class TeamrecApplicationTests {

    @Test
    void contextoDeveCarregar() {
    }
}
