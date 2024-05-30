package software.baller.jsonsnapshotmigration.domain.adult;

import lombok.*;
import software.baller.jsonsnapshotmigration.domain.child.Child;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdultV2 extends Adult {
    public UUID id;
    public String name;
    public Integer age;
    public Integer ageInMonths;
    public List<Child> children;
}
