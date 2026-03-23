using UnityEngine;

public class WebGLBridge : MonoBehaviour
{
    [SerializeField] private CameraController cameraController;
    [SerializeField] private NPCManager npcManager;
    [SerializeField] private EnvironmentManager environmentManager;
    [SerializeField] private DayNightCycle dayNightCycle;
    [SerializeField] private ShopManager shopManager;

    private void Awake()
    {
        cameraController ??= FindFirstObjectByType<CameraController>();
        npcManager ??= FindFirstObjectByType<NPCManager>();
        environmentManager ??= FindFirstObjectByType<EnvironmentManager>();
        dayNightCycle ??= FindFirstObjectByType<DayNightCycle>();
        shopManager ??= FindFirstObjectByType<ShopManager>();
    }

    private bool TryParseIndex(string indexText, out int index)
    {
        if (int.TryParse(indexText, out index))
        {
            return true;
        }

        Debug.LogWarning($"[WebGLBridge] Invalid region index: {indexText}");
        return false;
    }

    private bool TryParseFloat(string valueText, string fieldName, out float value)
    {
        if (float.TryParse(valueText, out value))
        {
            return true;
        }

        Debug.LogWarning($"[WebGLBridge] Invalid {fieldName}: {valueText}");
        return false;
    }

    private bool TryParseBool(string valueText, string fieldName, out bool value)
    {
        if (bool.TryParse(valueText, out value))
        {
            return true;
        }

        if (valueText == "1")
        {
            value = true;
            return true;
        }

        if (valueText == "0")
        {
            value = false;
            return true;
        }

        Debug.LogWarning($"[WebGLBridge] Invalid {fieldName}: {valueText}");
        value = false;
        return false;
    }

    public void SetCameraRegion(string indexText)
    {
        if (cameraController == null)
        {
            Debug.LogWarning("[WebGLBridge] CameraController reference is missing.");
            return;
        }

        if (TryParseIndex(indexText, out var index))
        {
            cameraController.SetViewByIndex(index);
        }
    }

    public void ReturnToMain(string _)
    {
        if (cameraController == null)
        {
            Debug.LogWarning("[WebGLBridge] CameraController reference is missing.");
            return;
        }

        cameraController.ReturnToMain();
    }

    public void SetCameraWindShake(string stateText)
    {
        if (cameraController == null)
        {
            Debug.LogWarning("[WebGLBridge] CameraController reference is missing.");
            return;
        }

        if (TryParseBool(stateText, "wind shake state", out var state))
        {
            cameraController.SetWindShake(state);
        }
    }

    public void SpawnShopAtIndex(string indexText)
    {
        if (shopManager == null)
        {
            Debug.LogWarning("[WebGLBridge] ShopManager reference is missing.");
            return;
        }

        if (TryParseIndex(indexText, out var index))
        {
            shopManager.SpawnShopAtIndex(index);
        }
    }

    public void ClearAllShops(string _)
    {
        if (shopManager == null)
        {
            Debug.LogWarning("[WebGLBridge] ShopManager reference is missing.");
            return;
        }

        shopManager.ClearAllShops();
    }

    public void SetStoreRegion(string indexText)
    {
        if (!TryParseIndex(indexText, out var index))
        {
            return;
        }

        if (shopManager != null)
        {
            shopManager.SpawnShopAtIndex(index);
        }
        else
        {
            Debug.LogWarning("[WebGLBridge] ShopManager reference is missing.");
        }

        if (cameraController != null)
        {
            cameraController.SetViewByIndex(index);
        }
        else
        {
            Debug.LogWarning("[WebGLBridge] CameraController reference is missing.");
        }
    }

    public void StartAmbientSpawning(string _)
    {
        if (npcManager == null)
        {
            Debug.LogWarning("[WebGLBridge] NPCManager reference is missing.");
            return;
        }

        npcManager.StartAmbientSpawning();
    }

    public void StopAmbientSpawning(string _)
    {
        if (npcManager == null)
        {
            Debug.LogWarning("[WebGLBridge] NPCManager reference is missing.");
            return;
        }

        npcManager.StopAmbientSpawning();
    }

    public void SpawnPopupVisitors(string payload)
    {
        if (npcManager == null)
        {
            Debug.LogWarning("[WebGLBridge] NPCManager reference is missing.");
            return;
        }

        npcManager.SpawnPopupVisitorsFromPayload(payload);
    }

    public void SpawnSinglePopupVisitor(string indexText)
    {
        if (npcManager == null)
        {
            Debug.LogWarning("[WebGLBridge] NPCManager reference is missing.");
            return;
        }

        if (TryParseIndex(indexText, out var index))
        {
            npcManager.SpawnGuestToDestination(index);
        }
    }

    public void SetCongestionLevel(string levelText)
    {
        if (npcManager == null)
        {
            Debug.LogWarning("[WebGLBridge] NPCManager reference is missing.");
            return;
        }

        if (int.TryParse(levelText, out var level))
        {
            npcManager.SetCongestionLevel(level);
            return;
        }

        Debug.LogWarning($"[WebGLBridge] Invalid congestion level: {levelText}");
    }

    public void SetWeather(string payload)
    {
        if (environmentManager == null)
        {
            Debug.LogWarning("[WebGLBridge] EnvironmentManager reference is missing.");
            return;
        }

        environmentManager.SetWeather(payload);
    }

    public void ClearWeather(string _)
    {
        if (environmentManager == null)
        {
            Debug.LogWarning("[WebGLBridge] EnvironmentManager reference is missing.");
            return;
        }

        environmentManager.SetWeather("Clear");
    }

    public void SetDay(string _)
    {
        if (dayNightCycle == null)
        {
            Debug.LogWarning("[WebGLBridge] DayNightCycle reference is missing.");
            return;
        }

        dayNightCycle.SetDay();
    }

    public void SetNight(string _)
    {
        if (dayNightCycle == null)
        {
            Debug.LogWarning("[WebGLBridge] DayNightCycle reference is missing.");
            return;
        }

        dayNightCycle.SetNight();
    }

    public void StartDay(string durationText)
    {
        if (dayNightCycle == null)
        {
            Debug.LogWarning("[WebGLBridge] DayNightCycle reference is missing.");
            return;
        }

        if (string.IsNullOrWhiteSpace(durationText))
        {
            dayNightCycle.StartDay(120f);
            return;
        }

        if (TryParseFloat(durationText, "day duration", out var duration))
        {
            dayNightCycle.StartDay(duration);
        }
    }

}
