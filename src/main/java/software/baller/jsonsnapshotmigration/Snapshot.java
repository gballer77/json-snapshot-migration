package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = SnapshotV1.class)
@JsonSubTypes({@JsonSubTypes.Type(name = "v1", value = SnapshotV1.class)})
public class Snapshot {
    SnapshotV1 get() {
        return (SnapshotV1) this;
    }
}
