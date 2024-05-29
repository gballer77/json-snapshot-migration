package software.baller.jsonsnapshotmigration;

import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildV1 extends Child {
    public UUID id;
    public String name;
    public String favoriteColor;
}
