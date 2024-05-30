package software.baller.jsonsnapshotmigration.domain.adult;

import lombok.*;
import software.baller.jsonsnapshotmigration.domain.child.Child;

import java.util.List;
import java.util.UUID;

//TODO remove after 2024-08-29

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated(since="2024-05-29")
public class AdultV1 extends Adult {
    public UUID id;
    public String name;
    public Integer age;
    public List<Child> children;
}
