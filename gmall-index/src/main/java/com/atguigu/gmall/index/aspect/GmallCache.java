package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {
    String prefix() default "";
    int timeout() default 5;
    int random() default 5;
    String lock() default "lock";

}
