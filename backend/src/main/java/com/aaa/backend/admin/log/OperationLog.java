package com.aaa.backend.admin.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志语义标注（可选）。
 *
 * <p>切面默认拦截所有 @PostMapping / @PutMapping / @DeleteMapping / @PatchMapping，
 * 模块与动作会按 Controller 类名、方法名、HTTP 动词自动推导；
 * 在需要更精确的业务名称时，用本注解覆盖。
 *
 * <pre>
 * &#64;OperationLog(module = "药品管理", action = "新增药品")
 * &#64;PostMapping
 * public ApiResponse&lt;Object&gt; create(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 业务模块名，留空则由 Controller 自动推导 */
    String module() default "";

    /** 操作名称，留空则由方法名 / HTTP 动词自动推导 */
    String action() default "";

    /** 为 true 时该接口不记录操作日志 */
    boolean ignore() default false;
}
