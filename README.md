<p align="center">
    <img src="./docs/images/icon.png" alt="Lightning logo" width="150" /><br>
</p>


# [Lightning](https://ebay.github.io/lightning/)
[Lightning](https://ebay.github.io/lightning/) is a Java based, [super fast](https://ebay.github.io/lightning/#benchmark), [multi-mode](https://ebay.github.io/lightning/#modes), [asynchronous, and distributed](https://ebay.github.io/lightning/#architecture) URL execution engine from eBay that delivers at scale.
<br>
Please refer [Detailed documentation](https://ebay.github.io/lightning/) for more information.


[![Release](https://img.shields.io/badge/Release-0.9.0-blue.svg)](https://img.shields.io/badge/Release-0.9.0-blue.svg)
<a href="https://ebay.github.io/lightning/" target="_blank"><img src="https://img.shields.io/website-up-down-green-red/http/shields.io.svg?label=Documentation" alt="Documentation"/></a>
<a href="https://ebay.github.io/lightning/apidocs/core/index.html" target="_blank"><img src="https://img.shields.io/website-up-down-green-red/http/shields.io.svg?label=Core%20API%20JavaDoc" alt="Core API"/></a>
<a href="https://ebay.github.io/lightning/apidocs/client/index.html" target="_blank"><img src="https://img.shields.io/website-up-down-green-red/http/shields.io.svg?label=Client%20API%20JavaDoc" alt="Client API"/></a>
[![CoreCov](https://img.shields.io/badge/Core%20Code%20Coverage-80-green.svg)](https://img.shields.io/badge/Core%20Code%20Coverage-80-green.svg)
[![ClientCov](https://img.shields.io/badge/Client%20Code%20Coverage-90-green.svg)](https://img.shields.io/badge/Client%20Code%20Coverage-90-green.svg)

# Usage
## Run In Embedded Mode
To configure lightning core as an embedded service within the application. <a href="https://ebay.github.io/lightning/#modes" target="_blank" >Click to Learn more about different Modes of Operation.</a>

```java
// Build LightningClient in Embedded Mode
final LightningClient client = new LightningClientBuilder().setEmbeddedMode(true).build();

// Create task List
final List<Task> lightningTasks = new ArrayList<>();
lightningTasks.add(new URLTask("http://www.ebay.com"));
lightningTasks.add(new URLTask("http://www.stubhub.com"));
lightningTasks.add(new URLTask("http://github.com/"));


// Submit task List with LightningResponseCallback and Timeout
final LightningResponseCallback callback = new LightningResponseCallback() {
    // Called when request could not complete within the given timeout
    @Override
    public void onTimeout(LightningResponse response) {
        System.out.println("Lightning request timed out.");
    }

    // Called when the request execution is completed
    @Override
    public void onComplete(LightningResponse response) {
        System.out.println("Request execution complete.");
        final int failureCount = response.getFailedResponses().size();
        if (failureCount > 0) {
            System.out.println("One or more requests failed.");
        } else {
            System.out.println("All results are successful with HTTP 200.");
            // Consume response...

        }
        System.out.println(response.prettyPrint());
    }
};

client.submitWithCallback(lightningTasks, callback, 5000);
```
<a href="https://ebay.github.io/lightning/#runEmbeddedModeMaven"  target="_blank"> Please find step-by-step here.</a>

## Run In Standalone Mode
To configure lightning core as an external service.

```java
//Build LightningClient and register with a core instance running @localhost on port 8989
LightningClient client = new LightningClientBuilder().addSeed("localhost").setCorePort(8989).build();

//Create tasks List
final List<Task> lightningTasks = new ArrayList<Task>();
lightningTasks.add(new URLTask("http://www.ebay.com"));
lightningTasks.add(new URLTask("http://www.stubhub.com"));
lightningTasks.add(new URLTask("http://github.com/"));

//Submit task List with LightningResponseCallback and Timeout
LightningResponseCallback callback = new LightningResponseCallback() {
    // Called when request could not complete within the given timeout
    @Override
    public void onTimeout(LightningResponse response) {
        log.info("Lightning request timed out.");
    }
    
    //Called when the request execution is completed
    @Override
    public void onComplete(LightningResponse response) {
        log.info("Request execution complete.");
        int failureCount = response.getFailedResponses().size();
        if (failureCount > 0) {
            log.info("One or more requests failed.");
        } else {
            log.info("All results are successful with HTTP 200.");
            //Consume response...
        }
        log.info(response.prettyPrint());
    }
};
client.submitWithCallback(tasks, callback, 5000);
```
<a href="https://ebay.github.io/lightning/#runStandaloneModeMaven"  target="_blank"> Please find step-by-step here.</a>
# Changelog

See [CHANGELOG.md](CHANGELOG.md)

# Contributors

Lightning is served to you by [Shankar Shukla](https://github.com/shankarshukla) and Site Engineering Tools Team at eBay Inc. We would like to give special thanks to our Manager Sham Kalwit for his vision, valuable guidance and encouragement.

# License

MIT
