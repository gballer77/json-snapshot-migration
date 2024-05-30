package software.baller.jsonsnapshotmigration.domain.snapshot;

import lombok.*;
import software.baller.jsonsnapshotmigration.domain.adult.Adult;

import java.util.Date;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotV1 extends Snapshot {
    public UUID id;
    public Integer version;
    public Date timestamp;
    public Adult adult;
}
