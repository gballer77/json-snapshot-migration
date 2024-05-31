This repository illustrates the usage of Jackson and Mapstruct to manage immutable snapshots saved to some sort of datastore, whether it be a database, cache, file, or something else.  These snapshots are intended to be deep copies of an aggregate, starting at the aggregate root, and are likely going to be heavily nested.  

Tech stack:
- Java 17
- Spring 3.3

## Background and required state of mind

Some of what you are about to see may lead you to ask questions like: why are we making this so complicated? Why arent we just mutating the field name?  Why keep around old versions of the entities? 

The short answer to this is, because you need to, for two primary reasons:

1) In building services for a system of systems, you cannot ensure that every service that relies on "this" one will be able to update at the same time. In fact, it is good practice to keep all services individually deployable.  To do this, we mustn't ever break our contracts with our consumers.  This is going to force us to continue to service old contracts while continuing to update our own logic.  This doesnt have to be indefinite, however.  In fact, its good practice to put an expiration date on any old contracts and ensure that consumers are off the old and onto the new by that deadline.  This will enable the team to go back and remove old code as time progresses.
2) Like any modern software application we want to reduce downtime and increase overall availability.  Doing this in modern platforms such as Tanzu Platform for Cloud Foundry or Kubernetes, requires rollout deployments.  In this deployment pattern, versions of your old application will remain online while your new versions spin up, both often operating out of the same database.  This means that between consecutive versions of our applications we cannot make any changes that would break the previous versions.  This means things like refactoring names in a single release are strictly forbidden.  This mindset should be taken for database migrations as well as JSON snapshots, like we are going to explore in this repo.  This mindset will also protect us in the event we need to roll back versions.

## Approach details

Let's assume we have a simple JSON snapshot payload:

```json
{
  "id": "a9018729-a154-4c51-aada-5811efee6d54",
  "version": 1,
  "timestamp": "1999-01-08 04:05:06",
  "adult": {
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

### Implementation

There are three different entities depicted above, `Snapshot` (or the snapshot envelope), `Adult`, and `Child`, which both make up the payload. Each object that will be a part of the deep copy will have at least two classes. 

- The first is a base class that all concrete version implementations will extend from.  This class will also be responsible for having the mapping logic for going up and down versions as needed.
- The second is the concrete version classes that defines the members of that object.  This will extend the base class and likely not have any additional functions.

The corresponding Java code for each of the entities looks like this:

<ins>**Snapshot Envelope**</ins>

```java
//Snapshot.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = SnapshotV1.class)
@JsonSubTypes({@JsonSubTypes.Type(name = "v1", value = SnapshotV1.class)})
public class Snapshot {
    SnapshotV1 get() {
        return (SnapshotV1) this;
    }
}
```
This class defines the base class for snapshot.  It also has a `get` method, which will return the concrete class of the latest version of this object. The `@JsonTypeInfo` tells Jackson how to tag your marshalled objects with a type.  Based on this definition we are going to be adding a `jsonType` field to every JSON object. We also define the default implementation, or object to unmarshall to as SnapshotV1, which is initially our only object.  This will come into play more later. Child would look something like this:

```json
{
  "jsonType": "v1",
  "id": "ae8efef6-0832-4d77-97fa-d73c406b51dc",
  "name": "Billy",
  "favoriteColor": "blue"
}
```

We also defined the `@JsonSubTypes` annotation which defines the possible classes to unmarshall to including their corresponding `jsonType`.  This will enable Jackson to recognize and properly parse multiple versions or subtypes of your JSON objects.

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
This class defines the concrete implementation of initial version of Snapshot called `SnapshotV1`.  This will likely have no other methods defined.  In future versions of Java that has `record` polymorphism, this could change from `class` to `record`.

<ins>**Adult**</ins>

```java
// Adult.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="jsonType", defaultImpl = AdultV1.class)
@JsonSubTypes({@Type(name = "v1", value = AdultV1.class)})
public class Adult {
    AdultV1 get() {
        return (AdultV1) this;
    }
}
```

Like the Snapshot base class, `Adult` has a single function currently for getting the latest version.

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
@JsonSubTypes({@JsonSubTypes.Type(name = "v1", value = ChildV1.class)})
public class Child {
    ChildV1 get() {
        return (ChildV1) this;
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

For the purposes of this example we will go through 2 iterations, or changes, to this schema.  For each iteration, we will show the new structure, and the object and mapping logic for each.

#### Iteration 1

The first change is going to be adding `age` to the children. For this we will need to modify the root `Child` class, and create a new `ChildV2` class.

```java
// Child.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = ChildV2.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = ChildV1.class),
        @JsonSubTypes.Type(name = "v2", value = ChildV2.class)
})
public class Child {
    ChildMapper mapper = Mappers.getMapper(ChildMapper.class);

    public ChildV2 get() throws IllegalArgumentException{
        if(this instanceof ChildV1 c) {
            return mapper.v1ToV2(c);
        } else if(this instanceof ChildV2) {
            return (ChildV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    // If this function is named `getV1` it will cause issues with Jackson Marshalling
    @Deprecated(since="2024-05-29")
    public ChildV1 v1() throws IllegalArgumentException{
        if(this instanceof ChildV1) {
            return (ChildV1) this;
        } else if(this instanceof ChildV2 v2) {
            return mapper.v2ToV1(v2);
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    @Mapper
    interface ChildMapper {
        @Mapping(target = "age", ignore = true)
        ChildV2 v1ToV2(ChildV1 c);

        ChildV1 v2ToV1(ChildV2 c);
    }
}
```
##### Changes
- updated the `defaultImpl` to `ChildV2`.  This will ensure that if we ever fall back to this version of the software, the most recent json payload is used.  Note that on the previous version the `defaultImpl` was set to `ChildV1`.  This means that in the event we have to rollback, having possibly even stored V2 versions of this payload, the code will continue to operate with the `ChildV1` fields.
- added an additional `@JsonSubTypes` value for `v2`.  Notice it now resolves to `ChildV2.class` which we will define in a second.
- updated the `get` function to return `ChildV2` as opposed to `ChildV1`.  We also now need to switch on object type, return `ChildV2` if its already of that type, and upgrade `ChildV1` to `ChildV2` if it was stored in the old JSON type.  For good measure we will throw an exception if neither of those things are true.
- added a method for getting the previous version of the payload.  Chances are, you are returning this payload to someone else via REST, which means you will want to provide a way for them to gracefully migrate off of those endpoints over a period of time. To do this, we will need to add a function to return previous versions that we still have need to process or return.  Notice that this is actually doing a downgrade, from V2 to V1. I recommend adding a `@Deprecated` tag to anything that you expect should not be used or soon be removed from your code.
- added a Mapstruct mapper to help with the conversions.  This is recommendation but not necessary.

**NOTE: In addition to adding an @Deprecated annotation, I also recommend creating a story to your backlog with a clear date to remind the team to go back and delete the old code once its no longer needed.**

**WARNING: You may be tempted, as I was, to rename the `v1()` method to `getV1()`.  If you do you will get an error when ObjectMapper trys to write the JSON.  I'm sure this is overcomable, however I did not spend the time on how to overcome it.**

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

The `ChildV2` class is in addition to the `ChildV1` class (to which you should also add `@Deprecated`).  As long as there are snapshots in our database of a certain version, we need to ensure that we keep the class around to decode it.

Our new JSON looks like this:
```json
{
  "jsonType": "v1",
  "id": "a9018729-a154-4c51-aada-5811efee6d54",
  "version": 1,
  "timestamp": "1999-01-08 04:05:06",
  "snapshot": {
    "jsonType": "v1",
    "id": "f41451b0-00e5-4bb5-a1ca-d01f493e6885",
    "name": "Bob",
    "age": 26,
    "children": [
      {
        "jsonType": "v2",
        "id": "ae8efef6-0832-4d77-97fa-d73c406b51dc",
        "name": "Billy",
        "favoriteColor": "blue",
        "age": 2
      }
    ]
  }
}
```

##### Accessing members 

Now that we've seen how the objects are stored, marshalled, and unmarshalled, lets talk a little bit about how they can be used in the code.  In order to work around the unknown concrete type or never ending casting problem, we introduced the `get` and `v<x>` methods to our base classes.  This enables us to use java `Optional` type syntax to access our concrete class.  The following shows how we would access the latest version of the snapshot payload for child and get the id.

```java
void accessChildId() {
    json.get().snapshot.get().children.get(0).get().id
}
```

We can also access an older version of the JSON payload if we wanted to with something like this:

```java
void accessChildId() {
    json.get().snapshot.get().children.get(0).v1();
}
```

##### Handling possible rollbacks

Now what if we roll this version out for a couple hours and determine we need to roll back to the previous version?  We have `V2` snapshots in our database, but when we roll back, we wont have the `V2` class to decode it.  That's correct, we don't, but we do have the `defaultImpl` set on the previous version to `ChildV1`, which means that in the event of a rollback in this instance, the application will continue to work because it will default to `ChildV1` and will continue to parse the fields it can, just ignoring age.

#### Iteration 2

In iteration 2, we're going to do something a little more difficult, we're going to mutate a field on Adult.  In this case, we are going to change the `age` field from age in years, to age in months.

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

Our new base Java class looks like this, with `AdultV2` becoming the default class:

```java
// Adult.java
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = AdultV2.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = AdultV1.class),
        @JsonSubTypes.Type(name = "v2", value = AdultV2.class)
})
public class Adult {
    Adult.AdultMapper mapper = Mappers.getMapper(Adult.AdultMapper.class);

    public AdultV2 get() throws IllegalArgumentException{
        if(this instanceof AdultV1 a) {
            return mapper.v1ToV2(a);
        } else if(this instanceof AdultV2) {
            return (AdultV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    // If this function is named `getV1` it will cause issues with Jackson Marshalling
    @Deprecated(since="2024-05-29")
    public AdultV1 v1() throws IllegalArgumentException {
        if(this instanceof AdultV1) {
            return (AdultV1) this;
        } else if(this instanceof AdultV2 v2) {
            return mapper.v2ToV1(v2);
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }

    @Mapper
    interface AdultMapper {
        @Named("yearsToMonths")
        default Integer yearsToMonths(Integer years) {
            return years * 12;
        }

        @Named("monthsToYears")
        default Integer monthsToYears(Integer months) {
            return months / 12;
        }

        @Mapping(target = "ageInMonths", source = "age", qualifiedByName = "yearsToMonths")
        AdultV2 v1ToV2(AdultV1 v1);

        @Mapping(target = "age", source = "ageInMonths", qualifiedByName = "monthsToYears")
        @Deprecated(since="2024-05-29")
        AdultV1 v2ToV1(AdultV2 v2);
    }
}
```
##### Changes
- All the changes we made were logically the same as child, with the addition of the Mapstruct named functions for converting years to months and months to years.

**NOTE: Java versions 19 and beyond have the ability to have a java switch statement for object type, upon upgrading to one of these later Java versions this code could be further cleaned up with this method.**


Which makes our snapshot JSON look like this:

```json
{
  "jsonType": "v1",
  "id": "a9018729-a154-4c51-aada-5811efee6d54",
  "version": 1,
  "timestamp": "1999-01-08 04:05:06",
  "snapshot": {
    "jsonType": "v2",
    "id": "f41451b0-00e5-4bb5-a1ca-d01f493e6885",
    "name": "Bob",
    "age": 26,
    "ageInMonths": 312,
    "children": [
      {
        "jsonType": "v2",
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
