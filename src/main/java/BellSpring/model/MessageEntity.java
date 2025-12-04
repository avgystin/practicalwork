package BellSpring.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("messages")
@Data
@NoArgsConstructor
public class MessageEntity {
    @Id
    private Long id;

    @Column("content")
    private String content;
}