import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.EnabledIf;

@EnabledIf("!java.lang.Boolean.getBoolean('bach.offline')")
@Retention(RetentionPolicy.RUNTIME)
@interface AssumeOnline {}
