### Approach details

Let's assume we have a simple JSON snapshot payload:

```json
{
  "id": "a9018729-a154-4c51-aada-5811efee6d54",
  "version": 1,
  "timestamp": "1999-01-08 04:05:06",
  "snapshot": {
    "id": "f41451b0-00e5-4bb5-a1ca-d01f493e6885",
    "name": "Bob",
    "age": 26,
    "children": [
      {
        "id": "ae8efef6-0832-4d77-97fa-d73c406b51dc",
        "name": "Billy",
        "favoriteColor": "blue"
      }
    ]
  }
}
```

#### Implementation

There are three different entities depicted above, `AdultSnapshot` (or the snapshot envelope), `Adult`, and `Child`, which both make up the payload. The corresponding Java code for these looks like this:

<ins>**Snapshot Envelope**</ins>
```java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = SnapshotV1.class)
@JsonSubTypes({@JsonSubTypes.Type(name = "v1", value = SnapshotV1.class)})
public class Snapshot {
    SnapshotV1 get() {
        return (SnapshotV1) this;
    }
}
```

```java
// SnapshotV1.java
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotV1 extends Snapshot {
    public UUID id;
    public Integer version;
    public Date timestamp;
    public Adult snapshot;
}
```

<ins>**Adult**</ins>

```java
// Adult.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="jsonType", defaultImpl = AdultV1.class)
@JsonSubTypes({@Type(name = "v1", value = AdultV1.class)})
public class Adult {}
```

```java
// AdultV1.java
@Data
public class AdultV1 extends Adult {
    public UUID id;
    public String name;
    public Integer age;
    public List<Child> children;
}
```

<ins>**Child**</ins>

```java
// Child.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = ChildV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = ChildV1.class)
})
public class Child {
    ChildV2 get() {
        return (ChildV2) this;
    }
}
```

```java
// ChildV1.java
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
```

For the purposes of this example we will go through 3 iterations, or changes, to this schema.  For each iteration, we will show the new structure, and the object and mapping logic for each.

##### Iteration 1

The first change is going to be adding age to the children. For this we will need to modify the root `Child` class, and create a new `ChildV2` class.

```java
// Child.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = ChildV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = ChildV1.class),
        @JsonSubTypes.Type(name = "v2", value = ChildV2.class)
})
public class Child {
    ChildV2 get() {
        if(this instanceof ChildV1 c) {
            return ChildV2.builder()
                    .id(c.id)
                    .favoriteColor(c.favoriteColor)
                    .name(c.name)
                    .age(null)
                    .build();
        } else if(this instanceof ChildV2) {
            return (ChildV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }
}
```

```java
// ChildV2.java
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
```

The `ChildV2` class is in addition to the `ChildV1` class.  As long as there are snapshots in our database of a certain version, we need to ensure that we keep the class around to decode it.

Our new JSON looks like this:
```json
{
  "@jsonType": "v1",
  "id": "a9018729-a154-4c51-aada-5811efee6d54",
  "version": 1,
  "timestamp": "1999-01-08 04:05:06",
  "snapshot": {
    "@jsonType": "v1",
    "id": "f41451b0-00e5-4bb5-a1ca-d01f493e6885",
    "name": "Bob",
    "age": 26,
    "children": [
      {
        "@jsonType": "v2",
        "id": "ae8efef6-0832-4d77-97fa-d73c406b51dc",
        "name": "Billy",
        "favoriteColor": "blue",
        "age": 2
      }
    ]
  }
}
```

###### "Up-conversion"

In order to keep the business logic of your app as simple as possible, you will only want to operate on a single version of your objects.  This means that before operating, we are going to need to up-convert the v1 object into the v2 object.  We do this with the `get` function in every base object class, such as `Child`.  The code then to access a member of child in the snapshot would look something like this:

```java
void accessChildId() {
    json.get().snapshot.get().children.get(0).get().id
}
```

###### Handling possible rollbacks

Now what if we roll this version out for a couple hours and determine we need to roll back to the previous version?  We have `V2` snapshots in our database, but when we roll back, we wont have the `V2` class to decode it.  That's correct, we don't, but we do have the `defaultImpl` set on the previous version to `ChildV1`, which means that in the event of a rollback in this instance, the application will continue to work because it will default to `ChildV1` and will continue to parse the fields it can, just ignoring age.

##### Iteration 2

The next release, Iteration 2, we are not changing anything with this JSON structure.  But there is still something we should change.  That is because when we release this, we want to fallback to the now verified working `ChildV2` class.  That way, in case we have to rollback again, we will still have the features around child age.  The only class that needs to change, is our base `Child` class, which now looks like this:

```java
// Child.java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "jsonType", defaultImpl = ChildV2.class)
@JsonSubTypes({
        @Type(name = "v1", value = ChildV1.class),
        @Type(name = "v2", value = ChildV1.class)
})
public class Child {
    //get function here as seen above
}
```

NOTE: If you think you might have to rollback more than one version, you can keep the default as `ChildV1` until you feel safe you won't rollback again.

##### Iteration 3

In iteration 3, we're going to do something a little more difficult, we're going to mutate a field on Adult.  In this case, we are going to change the `age` field from age in years, to age in months.

In order to continue to support a possible rollback though, we can't just change the value. If we do that and we do have to rollback, `AdultV1` won't know how to properly parse the value.  So instead of mutating it, we will simply add a new value for now. Our `AdultV2` class will look like this:

```java
// AdultV2.java
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
```

Our new base Java class looks like this, with `AdultV1` still being the default class:

```java
// Adult.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = AdultV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = AdultV1.class),
        @JsonSubTypes.Type(name = "v2", value = AdultV2.class)
})
public class Adult {
    AdultV2 get(){
        if(this instanceof AdultV1 a) {
            return AdultV2.builder()
                    .id(a.id)
                    .name(a.name)
                    .age(a.age)
                    .ageInMonths(a.age * 12)
                    .children(a.children)
                    .build();
        } else if(this instanceof AdultV2) {
            return (AdultV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }
}
```

Which makes our snapshot JSON look like this:

```json
{
  "@jsonType": "v1",
  "id": "a9018729-a154-4c51-aada-5811efee6d54",
  "version": 1,
  "timestamp": "1999-01-08 04:05:06",
  "snapshot": {
    "@jsonType": "v2",
    "id": "f41451b0-00e5-4bb5-a1ca-d01f493e6885",
    "name": "Bob",
    "age": 26,
    "ageInMonths": 312,
    "children": [
      {
        "@jsonType": "v2",
        "id": "ae8efef6-0832-4d77-97fa-d73c406b51dc",
        "name": "Billy",
        "favoriteColor": "blue",
        "age": 2
      }
    ]
  }
}
```

Because `AdultV1` is still our default class, even if we have to rollback, our application will still work, it will just utilize `age` instead of `ageInMonths`.  There is no clean way to "repurpose" a field name, in this case changing the `age` field.  Instead, we created a different uniquely named field that we are going to use moving forward.  This ensures that our application will always be backwards-compatible with old snapshots.

**NOTE: As soon as you are sure that you will not need to fall back to a version of the object that had the old definition of `age`, you can repurpose it.
