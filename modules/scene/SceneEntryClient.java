package com.daydream.scene;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 远端场景入场骨架：输入种子 → 请求场景清单 → 下载场景资源 → 把本地角色放到出生点。
 *
 * 约定：
 * - 场景服务器是可选网络扩展；本地对话推理与 TTS 离线主链路不依赖它。
 * - 网络请求、清单解析、资源下载必须在子线程；主线程只负责场景渲染与角色放置。
 * - 场景资源缓存到本地 scenes/<scene-id>/；角色外观 / 人设从本地 characters/ 目录加载。
 * - 服务器端点、可选认证信息由上层配置传入；本骨架不内置真实地址或密钥。
 */
public final class SceneEntryClient {

    /** 场景清单条目：资源相对路径、下载地址、校验和。 */
    public static final class SceneAsset {
        public final String path;
        public final String url;
        public final String sha256;

        public SceneAsset(String path, String url, String sha256) {
            this.path = path;
            this.url = url;
            this.sha256 = sha256;
        }
    }

    /** 出生点位置与朝向（position / rotation 为三轴数组，具体顺序以场景引擎约定为准）。 */
    public static final class SpawnPoint {
        public final float[] position;
        public final float[] rotation;

        public SpawnPoint(float[] position, float[] rotation) {
            this.position = position;
            this.rotation = rotation;
        }
    }

    private final File sceneCacheDir;
    private String serverEndpoint = "";

    public SceneEntryClient(File sceneCacheDir) {
        this.sceneCacheDir = sceneCacheDir;
    }

    public void setServerEndpoint(String endpoint) {
        this.serverEndpoint = endpoint == null ? "" : endpoint;
    }

    /**
     * 按种子请求场景清单。骨架阶段返回空清单；真实实现由 HTTP 客户端完成。
     *
     * @param seed        用户输入的场景种子
     * @param characterId 本地角色标识（Emma 或替换角色）
     * @return 场景资源列表；服务器不可用时返回空列表，调用方回退缓存 / 默认场景
     */
    public List<SceneAsset> requestSceneManifest(String seed, String characterId) {
        List<SceneAsset> assets = new ArrayList<>();
        // TODO: 构造请求 JSON（seed, characterId, scene_options, client_version）；
        // TODO: GET/POST serverEndpoint 对应场景接口，解析 manifest；
        // TODO: 校验响应 checksum；失败返回空列表，不抛出未校验资源。
        return assets;
    }

    /**
     * 下载场景资源到本地缓存目录。真实实现需校验 sha256；校验失败丢弃文件。
     *
     * @param assets 清单资源列表
     * @return 本地缓存目录下已就绪的场景资源文件列表
     */
    public List<File> downloadAssets(List<SceneAsset> assets) {
        List<File> ready = new ArrayList<>();
        if (sceneCacheDir != null && !sceneCacheDir.exists()) {
            sceneCacheDir.mkdirs();
        }
        // TODO: 逐项下载到 sceneCacheDir/<scene-id>/scene-assets/；写入前校验哈希。
        return ready;
    }

    /**
     * 角色入场：把本地角色放置到场景出生点。真实实现对接场景引擎的角色挂载 / spawn 方法。
     *
     * @param characterId 本地角色标识
     * @param spawn      清单给出的出生点
     */
    public void spawnCharacter(String characterId, SpawnPoint spawn) {
        // TODO: 从本地 characters/<character-id>/ 加载角色外观与人设；
        // TODO: 调用场景引擎 spawn / attach 方法，按 spawn.position、spawn.rotation 放置；
        // TODO: 角色加载与场景加载解耦：切换场景不必重载对话模型。
    }

    public File getSceneCacheDir() {
        return sceneCacheDir;
    }
}
