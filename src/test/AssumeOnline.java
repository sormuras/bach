import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Disabled("bach.offline")
@Retention(RetentionPolicy.RUNTIME)
@interface AssumeOnline {}
