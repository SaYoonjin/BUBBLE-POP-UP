using UnityEngine;
using System.Collections; 

public class DayNightCycle : MonoBehaviour
{
    public Light sunLight;
    public Gradient lightColor;

    [Header("스카이박스 세팅 (대장님 맞춤형)")]
    public float skyboxExposure = 1.4f; // 💡 노출도는 1.4로 평생 고정!
    
    [Tooltip("낮일 때 대기 두께 (기본 4.0)")]
    public float dayAtmosphereThickness = 4.0f;
    [Tooltip("밤일 때 대기 두께 (기본 0.4)")]
    public float nightAtmosphereThickness = 0.4f; 
    
    public float dayReflection = 1.0f;
    public float nightReflection = 0.05f;

    [Header("네온사인 (Emission) 설정")]
    public Material neonMaterial; 
    public float maxEmissionIntensity = 6.8f; 
    public float transitionTime = 3.0f; 
    [Range(0f, 1f)] public float lightOnThreshold = 0.75f; 

    private bool isRunning = false;
    private float timer = 0f;
    
    // ⭐ 기본 낮->밤 전환 시간 (필요시 수정)
    private float currentDuration = 120f; 
    
    private float originalIntensity;
    private float defaultYAngle;
    private float currentEmission = 0f;
    private float targetEmission = 0f;
    private Color cachedBaseColor = Color.white;

    void Start()
    {
        if (neonMaterial != null)
        {
            Color matColor = neonMaterial.GetColor("_EmissionColor");
            float maxColorComponent = Mathf.Max(matColor.r, Mathf.Max(matColor.g, matColor.b));
            cachedBaseColor = (maxColorComponent > 0f) ? (matColor / maxColorComponent) : Color.white; 
            neonMaterial.SetColor("_EmissionColor", cachedBaseColor * 0f);
        }

        if (sunLight == null) return;

        originalIntensity = sunLight.intensity;
        defaultYAngle = sunLight.transform.eulerAngles.y;
        
        SetDay(); 
    }

    public void SetDay()
    {
        if (sunLight == null) return;
        isRunning = false;
        timer = 0f;
        sunLight.intensity = originalIntensity;
        sunLight.transform.rotation = Quaternion.Euler(0f, defaultYAngle, 0f);

        if (lightColor != null && lightColor.colorKeys.Length > 0)
            sunLight.color = lightColor.Evaluate(0f);

        // 💡 낮 세팅: 밝기 1.4 고정, 두께 4.0!
        RenderSettings.skybox.SetFloat("_Exposure", skyboxExposure);
        RenderSettings.skybox.SetFloat("_AtmosphereThickness", dayAtmosphereThickness);
        RenderSettings.ambientIntensity = 1.0f;
        RenderSettings.reflectionIntensity = dayReflection; 
        targetEmission = 0f;
    }

    public void SetNight()
    {
        if (sunLight == null) return;
        isRunning = false;
        timer = currentDuration; 
        sunLight.intensity = 0f; 
        sunLight.transform.rotation = Quaternion.Euler(180f, defaultYAngle, 0f);

        if (lightColor != null && lightColor.colorKeys.Length > 0)
            sunLight.color = lightColor.Evaluate(1f);

        // 💡 밤 세팅: 밝기 1.4 고정, 두께 0.4!
        RenderSettings.skybox.SetFloat("_Exposure", skyboxExposure);
        RenderSettings.skybox.SetFloat("_AtmosphereThickness", nightAtmosphereThickness);
        RenderSettings.ambientIntensity = 0.1f;
        RenderSettings.reflectionIntensity = nightReflection; 
        targetEmission = maxEmissionIntensity;
    }

   public void StartDay(float duration)
    {
        if (sunLight == null) return;
        
        // 💡 [무적 방어막] 이미 시간이 흐르고 있다면 또 시작하지 않고 무시합니다!!
        if (isRunning) 
        {
            return; 
        }

        SetDay(); 
        currentDuration = duration; 
        isRunning = true; 
    }

    void Update()
    {
        if (neonMaterial != null)
        {
            float speed = maxEmissionIntensity / transitionTime; 
            currentEmission = Mathf.MoveTowards(currentEmission, targetEmission, speed * Time.deltaTime);
            neonMaterial.EnableKeyword("_EMISSION");
            neonMaterial.SetColor("_EmissionColor", cachedBaseColor * currentEmission);
        }

        if (!isRunning || sunLight == null) return;

        timer += Time.deltaTime;
        float progress = timer / currentDuration;
        
        float currentAngle = Mathf.Lerp(0f, 180f, progress);
        sunLight.transform.rotation = Quaternion.Euler(currentAngle, defaultYAngle, 0f);

        if (lightColor != null && lightColor.colorKeys.Length > 0)
            sunLight.color = lightColor.Evaluate(progress);
        else
            sunLight.color = Color.Lerp(Color.white, Color.red, progress);

        // ⭐ 핵심: 밝기는 1.4 유지, 두께만 스무스하게 깎아냅니다!
        RenderSettings.skybox.SetFloat("_Exposure", skyboxExposure);
        RenderSettings.skybox.SetFloat("_AtmosphereThickness", Mathf.Lerp(dayAtmosphereThickness, nightAtmosphereThickness, progress));
        
        RenderSettings.ambientIntensity = Mathf.Lerp(1.0f, 0.1f, progress);
        RenderSettings.reflectionIntensity = Mathf.Lerp(dayReflection, nightReflection, progress);

        // 💡 실시간으로 하늘 빛 반사 업데이트 (두께 변할 때 가끔 필요한 옵션)
        DynamicGI.UpdateEnvironment();

        if (progress >= lightOnThreshold) targetEmission = maxEmissionIntensity;

        if (timer >= currentDuration)
        {
            isRunning = false; 
            sunLight.intensity = 0f;
            sunLight.transform.rotation = Quaternion.Euler(180f, defaultYAngle, 0f);
            targetEmission = maxEmissionIntensity; 
        }
    }
}