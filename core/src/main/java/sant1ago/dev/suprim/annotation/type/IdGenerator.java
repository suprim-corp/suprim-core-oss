package sant1ago.dev.suprim.annotation.type;

/**
 * Interface for custom ID generation strategies.
 * Implement this to provide your own ID generation logic.
 *
 * <pre>{@code
 * public class CustomIdGenerator implements IdGenerator<String> {
 *     @Override
 *     public String generate() {
 *         return "prefix-" + UUIDUtils.v7().toString();
 *     }
 * }
 *
 * @Id(generator = CustomIdGenerator.class)
 * @Column(type = SqlType.UUID)
 * private String id;
 * }</pre>
 *
 * @param <T> the type of ID to generate (String, Long, UUID, etc.)
 */
@FunctionalInterface
public interface IdGenerator<T> {

    /**
     * Generate a new ID value.
     *
     * @return the generated ID
     */
    T generate();

    /**
     * Default no-op generator (manual assignment).
     */
    final class None implements IdGenerator<Object> {
        /**
         * Constructs a None generator.
         */
        public None() {
        }

        @Override
        public Object generate() {
            return null;
        }
    }
}
