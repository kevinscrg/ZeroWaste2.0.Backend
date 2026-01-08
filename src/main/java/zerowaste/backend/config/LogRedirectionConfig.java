package zerowaste.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.PrintStream;

@Configuration
public class LogRedirectionConfig {

    private static final Logger hibernateLog = LoggerFactory.getLogger("HibernateManual");
    private static final Logger soutLog = LoggerFactory.getLogger("SystemOut");
    private static final Logger errLog = LoggerFactory.getLogger("SystemErr");

    @PostConstruct
    public void init() {
        System.setOut(new PrintStream(System.out) {
            @Override
            public void println(String x) {
                redirect(x);
            }
            @Override
            public void print(String x) {
                redirect(x);
            }

            private void redirect(String x) {
                if (x == null) return;
                if (x.contains("Hibernate")) {
                    hibernateLog.info(x);
                } else {
                    soutLog.info(x);
                }
            }
        });

        System.setErr(new PrintStream(System.err) {
            @Override
            public void println(String x) {
                errLog.error(x);
            }
        });
    }
}