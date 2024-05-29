package software.baller.jsonsnapshotmigration;

import lombok.*;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdultV1 extends Adult {
    public UUID id;
    public String name;
    public Integer age;
    public List<ChildV1> children;
}
