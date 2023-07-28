## Dependency Information

The dependency FQDN is:

````
space.arim.libertybans:bans-api:{VERSION}
````

It is available from:

```
https://mvn-repo.arim.space/affero-gpl3/
```
<!-- tabs:start -->

#### **Maven**

With maven, this would be applied as follows:

```xml
<dependencies>
	<dependency>
		<groupId>space.arim.libertybans</groupId>
		<artifactId>bans-api</artifactId>
		<version>{INSERT_LATEST_VERSION}</version>
	</dependency>
	...
</dependencies>

<repositories>
	<repository>
		<id>arim-mvn-agpl3</id>
		<url>https://mvn-repo.arim.space/affero-gpl3/</url>
	</repository>
	...
</repositories>
```

#### **Gradle**

```gradle
repositories {
  ...
    maven { 
      name= 'arim-mvn-lgpl3' 
      url = 'https://mvn-repo.arim.space/lesser-gpl3/' 
    }
    maven { 
      name= 'arim-mvn-gpl3' 
      url = 'https://mvn-repo.arim.space/gpl3/' 
    }
    maven { 
      name= 'arim-mvn-agpl3' 
      url = 'https://mvn-repo.arim.space/affero-gpl3/' 
    }
  ...
}

dependencies {
  ...
    compileOnly "space.arim.libertybans:bans-api:{INSERT_LATEST_VERSION}"
}
```

<!-- tabs:end -->


### Javadoc

The javadocs are attached to the artifact and your IDE should be able to download them.

## API Usage

### Getting the instance

The first step to use the API is to get the instance of LibertyBans installed. This should be done once, such as when your plugin starts. Then, when access to the API is required, the instance you previously acquired should be passed.

```java
public class WikiExamples {

	private final Omnibus omnibus;
	private final LibertyBans libertyBans;

	public WikiExamples(Omnibus omnibus, LibertyBans libertyBans) {
		this.omnibus = omnibus;
		this.libertyBans = libertyBans;
	}
	
	public static WikiExamples create() {
		Omnibus omnibus = OmnibusProvider.getOmnibus();
		LibertyBans libertyBans = omnibus.getRegistry().getProvider(LibertyBans.class).orElseThrow();
		return new WikiExamples(omnibus, libertyBans);
	}
	
}
```

### Adding Punishments

Creating punishments starts with a `DraftPunishment`, which is obtained from `PunishmentDrafter`. The draft punishment is sent to LibertyBans, which will add the punishment to the database and return a full `Punishment`.

Building on the previous example, here's what that might look like.

```java
public void banPlayerUsingLibertyBans(UUID uuidToBan) {

	PunishmentDrafter drafter = libertyBans.getDrafter();

	DraftPunishment draftBan = drafter
		.draftBuilder()
		.type(PunishmentType.BAN)
		.victim(PlayerVictim.of(uuidToBan))
		.reason("Because I said so")
		.build();

	draftBan.enactPunishment().thenAcceptSync((punishment) -> {
		// In this example it is assumed you have a logger
		// You should not copy and paste examples verbatim
		if (punishment == null) {
			logger.info("UUID {} is already banned", uuidToBan);
			return;
		}
		logger.info("ID of the enacted punishment is {}", punishment.getIdentifier());
	});
}
```

### Getting Punishments

There is a comprehensive punishment selection API which allows you to retrieve punishments with certain details. For example, to get all mutes issued by a specific staff member:

```java
public ReactionStage<List<Punishment>> getMutesFrom(UUID staffMemberUuid) {
	return libertyBans.getSelector()
		.selectionBuilder()
		.type(PunishmentType.MUTE)
		.operator(PlayerOperator.of(staffMemberUuid))
		.build()
		.getAllSpecificPunishments();
}
```

If you want to determine whether a player is muted, you should select *applicable* punishments (i.e., which includes user and IP mutes):

```java
public ReactionStage<Optional<Punishment>> getMutesApplyingTo(UUID playerUuid, InetAddress playerAddress) {
	return libertyBans.getSelector()
		.selectionByApplicabilityBuilder(playerUuid, playerAddress)
		.type(PunishmentType.MUTE)
		.build()
		.getFirstSpecificPunishment();
}
```

### Removing Punishments

Assuming you already have a `Punishment` instance, `Punishment#undoPunishment` will remove the punishment easily.

If you don't have a punishment instance on hand, you can use the `PunishmentRevoker`:

```java
public ReactionStage<?> revokeBanFor(UUID bannedPlayer) {
	PunishmentRevoker revoker = libertyBans.getRevoker();

	// Relies on the fact a single victim can only have 1 active ban
        RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(
		PunishmentType.BAN, PlayerVictim.of(bannedPlayer)
        );
	return revocationOrder.undoPunishment().thenAccept((undone) -> {
		if (undone) {
			// ban existed and was undone
		} else {
			// there was no ban
		}
	});
}
```

### Listening to Events

Suppose you want to run code when a player is punished. Here's how you'd do that using `PunishEvent`:

```java
public void listenToPunishEvent() {
	EventConsumer<PunishEvent> listener = new EventConsumer<>() {
		@Override
        public void accept(PunishEvent event) {
			logger.info("Listening to punish event {}", event);
		}
	};
	omnibus.getEventBus().registerListener(PunishEvent.class, ListenerPriorities.NORMAL, listener);
}
```

The full list of events is in the javadoc.

### ReactionStage

As you may have noticed, ReactionStage is a subinterface of CompletionStage. It can be converted to a CentralisedFuture, which is a subclass of CompletableFuture, via `toCompletableFuture`. **If you have not used CompletableFuture or CompletionStage before, you will need to understand those classes first.**

ReactionStage/CentralisedFuture is analogous to CompletionStage/CompletableFuture. They're almost exactly the same - ReactionStage/CentralisedFuture extend CompletionStage/CompletableFuture and add a couple more methods.

You can use CompletionStage/CompletableFuture to decouple your plugin from LibertyBans, but you may also want to take advantage of the *Sync* methods on CentralisedFuture.

The sync methods allow you to run callbacks and operations on the main thread similarly how to the *Async* methods run on the common ForkJoinPool. Additionally, CentralisedFuture#join is enhanced to prevent deadlocks should you use `join()`, `get()`, or `get(long, TimeUnit)` while on the main thread. This means that if you have an intermediate operation which depends on the main thread, you can await its completion even if on the main thread itself:

```java
CentralisedFuture<?> future;
future.thenRunSync(() -> { // thenRunSync is not available with CompletableFuture
 // do operation on main thread
}).thenRunAsync(() -> {
 // do async operation
}).join(); // Calling join() will cause a deadlock when using a plain CompletableFuture
```
