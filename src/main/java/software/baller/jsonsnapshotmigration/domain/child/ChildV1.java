package software.baller.jsonsnapshotmigration.domain.child;

import lombok.*;

import java.util.UUID;

//TODO remove after 2024-08-29

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated(since="2024-05-29")
public class ChildV1 extends Child {
    public UUID id;
    public String name;
    public String favoriteColor;
}
