package software.baller.jsonsnapshotmigration;

import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildV2 extends Child{
    public UUID id;
    public String name;
    public String favoriteColor;
    public Integer age;
}
