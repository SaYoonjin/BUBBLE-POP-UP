using UnityEngine;
using System.Collections; 

public class EnvironmentManager : MonoBehaviour
{
    [Header("날씨 프리팹 (비, 눈, 모래바람, 강풍 모래)")]
    public GameObject rainPrefab;
    public GameObject snowPrefab;
    public GameObject dustPrefab; 
    public GameObject sandPrefab; // ⭐ 이번에 새로 추가된 강풍용 Sand 파티클!

    [Header("비/눈이 내릴 위치 5곳")]
    public Transform[] targetLocations;

    [Tooltip("하루(날씨)가 유지되는 시간")]
    public float weatherDuration = 20f;

    [Header("카메라 연출")]
    public CameraController cameraController;

    [Header("날씨 효과음 (사운드)")]
    public AudioClip rainSound;  
    public AudioClip windSound;  
    public AudioClip snowSound;  

    private AudioSource weatherAudioSource;

    void Awake()
    {
        weatherAudioSource = gameObject.AddComponent<AudioSource>();
        weatherAudioSource.spatialBlend = 0.0f;
        weatherAudioSource.loop = true; 
    }

    void Start()
    {
        Application.targetFrameRate = 60;
        ResetAllWeather();
    }

    public void SetWeather(string command)
    {
        if (string.IsNullOrWhiteSpace(command))
        {
            Debug.LogWarning("[EnvironmentManager] SetWeather 호출값이 비어있습니다.");
            return;
        }

        ResetAllWeather();

        string[] parts = command.Split(',');
        string weatherType = parts[0].Trim();
        int locationIndex = 0;

        if (parts.Length > 1)
        {
            int.TryParse(parts[1], out locationIndex); 
        }

        switch (weatherType)
        {
            case "Rain":
            case "rain":
                PlayWeatherSound(rainSound); 
                if (rainPrefab != null)
                {
                    MoveToLocation(rainPrefab, locationIndex);
                    rainPrefab.SetActive(true);

                    ParticleSystem[] allParticles = rainPrefab.GetComponentsInChildren<ParticleSystem>();
                    foreach (ParticleSystem ps in allParticles)
                    {
                        ps.Play(); 
                    }
                }
                break;

            case "Snow":
            case "snow":
                PlayWeatherSound(snowSound); 
                if (snowPrefab != null)
                {
                    snowPrefab.SetActive(true); 
                    MoveToLocation(snowPrefab, locationIndex);
                    
                    ParticleSystem[] allSnowParticles = snowPrefab.GetComponentsInChildren<ParticleSystem>();
                    foreach (ParticleSystem ps in allSnowParticles)
                    {
                        ps.Play(); 
                    }
                }
                else
                {
                    Debug.LogWarning("[EnvironmentManager] snowPrefab이 비어있습니다.");
                }
                break;

            case "Fog":
            case "fog":
                RenderSettings.fog = true;
                RenderSettings.fogColor = new Color(0.6f, 0.6f, 0.6f);
                RenderSettings.fogDensity = 0.04f;
                break;

            case "Dust":
            case "dust":
                PlayWeatherSound(windSound); 
                RenderSettings.fog = true;
                RenderSettings.fogColor = new Color(0.7f, 0.5f, 0.3f);
                RenderSettings.fogDensity = 0.01f;

                if (dustPrefab != null && targetLocations != null && locationIndex >= 0 && locationIndex < targetLocations.Length)
                {
                    Transform camTarget = targetLocations[locationIndex];
                    if (camTarget != null)
                    {
                        Vector3 dustPos = camTarget.position + (camTarget.forward * 10f) + (camTarget.right * 30f);
                        dustPos.y = 5.0f; 
                        dustPrefab.transform.position = dustPos;

                        float targetYaw = camTarget.eulerAngles.y + 210.0f; 
                        dustPrefab.transform.rotation = Quaternion.Euler(-90f, targetYaw, 0f);

                        dustPrefab.SetActive(true);
                        ParticleSystem[] allDust = dustPrefab.GetComponentsInChildren<ParticleSystem>();
                        foreach (ParticleSystem ps in allDust)
                        {
                            ps.Play();
                        }
                    }
                }
                break;

            case "Wind": 
            case "wind":
                PlayWeatherSound(windSound); 
                
                // 1. 카메라 강풍 흔들림 켜기
                if (cameraController != null)
                {
                    cameraController.SetWindShake(true);
                }

                // ⭐ 2. Dust와 완벽하게 똑같은 위치/각도로 Sand 파티클 쏘기!
                if (sandPrefab != null && targetLocations != null && locationIndex >= 0 && locationIndex < targetLocations.Length)
                {
                    Transform camTarget = targetLocations[locationIndex];
                    if (camTarget != null)
                    {
                        // 위치 계산 (Dust와 동일)
                        Vector3 sandPos = camTarget.position + (camTarget.forward * 10f) + (camTarget.right * 30f);
                        sandPos.y = 5.0f; 
                        sandPrefab.transform.position = sandPos;

                        // 각도 계산 (Dust와 동일)
                        float targetYaw = camTarget.eulerAngles.y + 210.0f; 
                        sandPrefab.transform.rotation = Quaternion.Euler(-90f, targetYaw, 0f);

                        // 파티클 켜기 및 재생
                        sandPrefab.SetActive(true);
                        ParticleSystem[] allSand = sandPrefab.GetComponentsInChildren<ParticleSystem>();
                        foreach (ParticleSystem ps in allSand)
                        {
                            ps.Play();
                        }
                    }
                }
                break;

            case "Clear":
            case "clear":
                break;

            default:
                Debug.LogWarning($"[EnvironmentManager] 지원하지 않는 날씨 타입: {weatherType}. Snow/Rain/Fog/Dust/Wind/Clear 중 하나를 사용하세요.");
                return;
        }

        StopAllCoroutines(); 
        StartCoroutine(WeatherTimer());
    }

    [Header("날씨 이동 세부 설정")]
    public float backwardOffset = 20.0f; 

    private void MoveToLocation(GameObject weatherObj, int index)
    {
        if (weatherObj == null || targetLocations == null || targetLocations.Length == 0) return;

        if (index >= 0 && index < targetLocations.Length && targetLocations[index] != null)
        {
            Vector3 targetPos = targetLocations[index].position;

            targetPos += targetLocations[index].forward * backwardOffset;
            targetPos.y += 20.0f; 

            weatherObj.transform.position = targetPos;
        }
    }

    private void PlayWeatherSound(AudioClip clip)
    {
        if (weatherAudioSource != null && clip != null)
        {
            weatherAudioSource.clip = clip;
            weatherAudioSource.Play();
        }
    }

    private void ResetAllWeather()
    {
        if (rainPrefab != null) rainPrefab.SetActive(false);
        if (snowPrefab != null) snowPrefab.SetActive(false);
        if (dustPrefab != null) dustPrefab.SetActive(false); 
        
        // ⭐ 초기화할 때 Sand 파티클도 같이 꺼주기
        if (sandPrefab != null) sandPrefab.SetActive(false); 

        RenderSettings.fog = false;
        
        if (cameraController != null)
        {
            cameraController.SetWindShake(false);
        }

        if (weatherAudioSource != null)
        {
            weatherAudioSource.Stop();
        }

        Debug.Log("[EnvironmentManager] ResetAllWeather 수행: 모든 날씨/효과음/흔들림 정지");
    }

    private IEnumerator WeatherTimer()
    {
        yield return new WaitForSeconds(weatherDuration); 
        ResetAllWeather(); 
    }

    [ContextMenu("테스트: 강풍 켜기 (흔들림 ON)")]
    public void TestWindOn()
    {
        SetWeather("Wind"); 
    }

    [ContextMenu("테스트: 강풍 끄기 (흔들림 OFF)")]
    public void TestWindOff()
    {
        ResetAllWeather();
    }
}