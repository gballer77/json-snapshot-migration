package software.baller.jsonsnapshotmigration;

import lombok.*;

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
    public AdultV1 snapshot;
}
