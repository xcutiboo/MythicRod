with open('mythicrod-common/src/main/java/io/xcutiboo/mythicrod/api/platform/PlatformLocation.java', 'r') as f:
    content = f.read()

content = content.replace('public interface PlatformLocation {', 'public class PlatformLocation {\n    private final String worldName;\n    private final double x, y, z;\n    private final float yaw, pitch;\n\n    public PlatformLocation(String worldName, double x, double y, double z, float yaw, float pitch) {\n        this.worldName = worldName;\n        this.x = x;\n        this.y = y;\n        this.z = z;\n        this.yaw = yaw;\n        this.pitch = pitch;\n    }\n')

with open('mythicrod-common/src/main/java/io/xcutiboo/mythicrod/api/platform/PlatformLocation.java', 'w') as f:
    f.write(content)
