package backend.dev.PLA.annotation;

import backend.dev.PLA.enums.CefrLevel;
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface level {
    CefrLevel value() default CefrLevel.A1;
}
