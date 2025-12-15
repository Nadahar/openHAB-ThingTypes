# Thing Types File Provider add-on

This add-on parses XML files placed in `$OPENHAB_CONF/thingtypes` and provides the resulting `ThingType`, `ChannelType` and `ChannelGroupType` definitions to the openHAB registries.
Use this to make custom or third‑party thing definitions immediately available without waiting for releases.

## Quick start

1. Create the folder if it does not exist:
   - Linux/macOS: `mkdir -p $OPENHAB_CONF/thingtypes`
   - Windows (PowerShell): `mkdir $env:OPENHAB_CONF\thingtypes`
2. Place one or more XML files with Thing definitions in that folder.
3. The add‑on will load the definitions and publish them to the registries.

## Prerequisites

- Compatible openHAB version: check the add‑on compatibility for your openHAB release.
- `$OPENHAB_CONF` must be defined and reachable by the openHAB process.
- XML files must follow the same syntax as described in the openHAB [Binding Definitions](https://www.openhab.org/docs/developer/bindings/thing-xml.html) documentation.

## File format / Example

Save files with `.xml` extension. Example (very small excerpt):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<thing-descriptions>
  <thing-type id="bindingid:device-type" label="Device Label">
    <channels>
      <channel id="switch" typeId="switch" />
    </channels>
  </thing-type>
</thing-descriptions>
```

Place it as:

- `$OPENHAB_CONF/thingtypes/my-device.xml`

## Intended Use

This add-on has been written to solve a challenge when using the [ZWave binding](https://www.openhab.org/addons/bindings/zwave/), but it is not limited to this use.
It provides definitions system-wide.

The challenge when using the ZWave binding is that device definitions must be created in the [Z-Wave Device Database](https://opensmarthouse.org/zwavedatabase), but there's no readily available way to actually use the resulting XML files.
Instead, one must wait for the next release, or use a snapshot build.
This is a slow cycle, especially if there's a need to tweak aspects of the definition after testing.

Another potential challenge is when running an older version of openHAB, which means that the updated definitions won't be made available at all, and a snapshot build probably won't work.

By installing this add-on and saving the XML files that can be exported from the device database in `$OPENHAB_CONF/thingtypes`, you can make these definitions available to your system immediately.

## Behavior & Limitations

- This add-on provides definitions system‑wide and can be used by any binding or other system component.
- Lack of priority: openHAB has no built‑in priority between sources. If a binding provides the same `ThingTypeUID`, you cannot define which source is used.
- ZWave binding: the [ZWave binding](https://www.openhab.org/addons/bindings/zwave/) caches device definitions aggressively at startup.
Adding or changing files at runtime will update openHAB’s registries, but will not affect ZWave binding behavior until openHAB is restarted.

## Troubleshooting

- Nothing appears after placing files:
  - Verify file syntax and extension `.xml`.
  - Check openHAB logs for parsing errors (look for the add‑on name).
  - Confirm that `$OPENHAB_CONF/thingtypes` is readable by openHAB.
- Device still not recognized (Z-Wave):
  - Restart openHAB to force the ZWave binding to re‑read definitions.
