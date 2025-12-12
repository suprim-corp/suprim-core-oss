/**
 * Suprim annotations for SQL entity mapping.
 *
 * <p>These annotations are processed at compile-time to generate
 * type-safe metamodel classes (Entity_) for query building.
 *
 * <p>Core annotations:
 * <ul>
 *   <li>{@link sant1ago.dev.suprim.annotation.entity.Entity} - Mark class as database entity</li>
 *   <li>{@link sant1ago.dev.suprim.annotation.entity.Table} - Specify table name and schema</li>
 *   <li>{@link sant1ago.dev.suprim.annotation.entity.Column} - Define column mapping</li>
 *   <li>{@link sant1ago.dev.suprim.annotation.entity.Id} - Mark primary key field</li>
 *   <li>{@link sant1ago.dev.suprim.annotation.entity.JsonbColumn} - PostgreSQL JSONB support</li>
 * </ul>
 *
 * @see sant1ago.dev.suprim.annotation.entity.Entity
 */
package sant1ago.dev.suprim.annotation;
