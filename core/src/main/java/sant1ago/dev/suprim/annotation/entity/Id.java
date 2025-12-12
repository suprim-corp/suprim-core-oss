package sant1ago.dev.suprim.annotation.entity;

import sant1ago.dev.suprim.annotation.type.GenerationType;
import sant1ago.dev.suprim.annotation.type.IdGenerator;

import java.lang.annotation.*;

/**
 * Marks a field as the primary key with optional generation strategy.
 *
 * <pre>{@code
 * // Built-in strategy: UUID v7
 * @Id(strategy = GenerationType.UUID_V7)
 * @Column(type = SqlType.UUID)
 * private String id;
 *
 * // Built-in strategy: auto-increment
 * @Id(strategy = GenerationType.IDENTITY)
 * @Column(type = SqlType.BIGINT)
 * private Long id;
 *
 * // Custom generator (your own logic)
 * @Id(generator = MyUuidGenerator.class)
 * @Column(type = SqlType.UUID)
 * private String id;
 *
 * // Manual assignment (default)
 * @Id
 * private Long id;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Id {

    /**
     * Built-in generation strategy.
     * Ignored if a custom generator is specified.
     * Default is NONE (manual assignment).
     *
     * @return the generation strategy
     */
    GenerationType strategy() default GenerationType.NONE;

    /**
     * Custom ID generator class.
     * Takes precedence over strategy if specified.
     * Must implement {@link IdGenerator} interface.
     *
     * @return the custom generator class
     */
    Class<? extends IdGenerator<?>> generator() default IdGenerator.None.class;

    /**
     * Sequence name for SEQUENCE strategy.
     * Only used when strategy = GenerationType.SEQUENCE.
     *
     * @return the sequence name
     */
    String sequence() default "";
}
