package BellSpring.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private Long id;

    @Column("order_uuid")
    private String orderUuid;

    @Column("session_id")
    private String sessionId;

    @Column("product_name")
    private String productName;

    @Column("quantity")
    private Integer quantity;

    @Column("unit_price")
    private Integer unitPrice;

    @Column("total_price")
    private Integer totalPrice;

    @Column("created_at")
    private LocalDateTime createdAt;
}