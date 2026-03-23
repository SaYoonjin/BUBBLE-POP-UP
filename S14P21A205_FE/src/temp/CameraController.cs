using UnityEngine;
using Unity.Cinemachine; // ⭐ 시네머신 3.x 필수

public class CameraController : MonoBehaviour
{
    public static CameraController Instance;

    [Header("시네머신 카메라 연결")]
    [Tooltip("평소에 8개 구역을 비출 기본 시네머신 카메라 (MainCam_Default)")]
    public CinemachineCamera mainZoneCam; 
    [Tooltip("도둑 발생 시 줌인할 시네머신 카메라 (CloseUpCam_Target)")]
    public CinemachineCamera closeUpCam;  

    [Header("캠 뷰 포인트 (8개 구역)")]
    public Transform viewCity1; 
    public Transform viewCity2;
    public Transform viewCity3;
    public Transform viewCity4;
    public Transform viewCity5;
    public Transform viewCity6; 
    public Transform viewCity7; 
    public Transform viewCity8; 

    // ⭐ [새로 추가된 자유 비행(탑뷰) 기능] ⭐
    [Header("자유 비행(탑뷰) 설정")]
    public bool isFreeMode = false; 
    public float moveSpeed = 30f;
    public float zoomSpeed = 3000f; // 시네머신은 공간이 넓을 수 있어 줌 속도를 높였습니다.
    public Vector3 topViewRotation = new Vector3(60f, 0f, 0f); // 내려다보는 각도

    [Header("가두리 양식장 (맵 이탈 방지)")]
    public float minX = -100f; public float maxX = 100f; 
    public float minZ = -100f; public float maxZ = 100f; 
    public float minY = 10f;   // 최대 줌인 (바닥)
    public float maxY = 60f;   // 최대 줌아웃 (하늘)

    private Transform currentZoneView; // 스페이스바를 껐을 때 돌아갈 '원래 구역' 기억용

    [Header("카메라 흔들림 (강풍 연출)")]
    public float windShakeAmount = 0.5f; 
    public float windShakeSpeed = 2.0f;  
    
    private bool isWindShaking = false;
    private Quaternion baseRotation; 
    private float noiseSeedX;
    private float noiseSeedY;

    public float zoomSmoothness = 10f; // 줌이 미끄러지는 정도 (낮을수록 더 미끄러짐)
    private float targetY;             // 마우스 휠이 지시한 '진짜 목표 높이'

    private int closeUpDefaultPriority;

    private void Awake()
    {
        Instance = this;

        noiseSeedX = Random.Range(0f, 100f);
        noiseSeedY = Random.Range(0f, 100f);

        if (closeUpCam != null)
        {
            closeUpDefaultPriority = closeUpCam.Priority; 
        }

        // 시작할 때 기본값을 1번 구역으로 기억해둡니다.
        currentZoneView = viewCity1;
    }

    private void Start()
    {
        if (mainZoneCam != null)
        {
            baseRotation = mainZoneCam.transform.rotation;
        }
    }

    // ⭐ [추가됨] 스페이스바 입력 & 자유 이동 처리
    private void Update()
    {
        if (Input.GetKeyDown(KeyCode.Space))
        {
            ToggleFreeMode();
        }

        if (isFreeMode && mainZoneCam != null)
        {
            HandleFreeMovement();
        }
    }

    private void LateUpdate()
    {
        if (isWindShaking && mainZoneCam != null)
        {
            float noiseX = (Mathf.PerlinNoise(Time.time * windShakeSpeed + noiseSeedX, 0f) * 2f - 1f) * windShakeAmount;
            float noiseY = (Mathf.PerlinNoise(0f, Time.time * windShakeSpeed + noiseSeedY) * 2f - 1f) * windShakeAmount;

            Quaternion shakeOffset = Quaternion.Euler(noiseX, noiseY, 0f);
            mainZoneCam.transform.rotation = baseRotation * shakeOffset;
        }
    }

  private void ToggleFreeMode()
    {
        if (mainZoneCam == null) return;
    
        isFreeMode = !isFreeMode;

        if (isFreeMode)
        {
            // ⭐ [핵심 추가 1] 시네머신이 멱살 잡고 있는 걸 강제로 풀어버립니다!
            mainZoneCam.Follow = null;
            mainZoneCam.LookAt = null;

            // ⭐ [핵심 추가 2] 스페이스바 누르면 하늘 위로 쑤욱! 올라가서 확실하게 티가 나게 만듭니다!
            Vector3 pos = mainZoneCam.transform.position;
            pos.y = 40f; // 탑뷰 전용 높은 고도
            mainZoneCam.transform.position = pos;

            // ⭐⭐⭐ [바로 여기입니다!!] 스무스 줌을 위해 '목표 높이'도 40으로 똑같이 맞춰줍니다!
            targetY = 40f; 

            // 시점 아래로 꺾기
            mainZoneCam.transform.rotation = Quaternion.Euler(topViewRotation);
            baseRotation = Quaternion.Euler(topViewRotation);
            
          
        }
        else
        {
            if (currentZoneView != null)
            {
                mainZoneCam.transform.SetPositionAndRotation(currentZoneView.position, currentZoneView.rotation);
                baseRotation = currentZoneView.rotation;
            }
           
        }
    }

 private void HandleFreeMovement()
    {
        float h = Input.GetAxis("Horizontal"); 
        float v = Input.GetAxis("Vertical");   
        float scroll = Input.GetAxis("Mouse ScrollWheel"); 

        Vector3 pos = mainZoneCam.transform.position;

        // ⭐ 1. 카메라의 '현재 높이'가 아니라, '목표 높이(targetY)'를 휠로 조절합니다!
        targetY -= scroll * zoomSpeed * Time.deltaTime;
        targetY = Mathf.Clamp(targetY, minY, maxY); // 목표 높이도 가두리 안쪽으로 막기

        // ⭐ 2. 현재 카메라 높이(pos.y)가 목표 높이(targetY)를 향해 부드~럽게 미끄러지듯 따라갑니다!
        pos.y = Mathf.Lerp(pos.y, targetY, Time.deltaTime * zoomSmoothness);

        // 3. X, Z 이동
        pos.x += h * moveSpeed * Time.deltaTime;
        pos.z += v * moveSpeed * Time.deltaTime;

        // 4. 고무줄 가두리 양식장 (현재 카메라 높이 pos.y 기준)
        float t = Mathf.InverseLerp(minY, maxY, pos.y); 
        float extraSpace = 40f; 

        float currentMinX = minX - (extraSpace * (1f - t));
        float currentMaxX = maxX + (extraSpace * (1f - t));
        float currentMinZ = minZ - (extraSpace * (1f - t));
        float currentMaxZ = maxZ + (extraSpace * (1f - t));

        pos.x = Mathf.Clamp(pos.x, currentMinX, currentMaxX);
        pos.z = Mathf.Clamp(pos.z, currentMinZ, currentMaxZ);

        mainZoneCam.transform.position = pos;
    }

    // ===================================================
    // 1. 강풍 켜기/끄기 
    // ===================================================
    public void SetWindShake(bool state)
    {
        if (isWindShaking == state) return;
        isWindShaking = state;

        if (mainZoneCam == null) return;

        if (state) 
            baseRotation = mainZoneCam.transform.rotation;
        else 
            mainZoneCam.transform.rotation = baseRotation; 
    }

    // ===================================================
    // 2. 8개 구역 뷰 이동 
    // ===================================================
    public void SetViewCity1() => ApplyView(viewCity1, "카메라 위치 1");
    public void SetViewCity2() => ApplyView(viewCity2, "카메라 위치 2");
    public void SetViewCity3() => ApplyView(viewCity3, "카메라 위치 3");
    public void SetViewCity4() => ApplyView(viewCity4, "카메라 위치 4");
    public void SetViewCity5() => ApplyView(viewCity5, "카메라 위치 5");
    public void SetViewCity6() => ApplyView(viewCity6, "카메라 위치 6");
    public void SetViewCity7() => ApplyView(viewCity7, "카메라 위치 7");
    public void SetViewCity8() => ApplyView(viewCity8, "카메라 위치 8");

    public void SetView1() => SetViewCity1();
    public void SetView2() => SetViewCity2();
    public void SetView3() => SetViewCity3();
    public void SetView4() => SetViewCity4();
    public void SetView5() => SetViewCity5();
    public void SetView6() => SetViewCity6();
    public void SetView7() => SetViewCity7();
    public void SetView8() => SetViewCity8();

    public void SetViewByIndex(int index)
    {
        switch (index)
        {
            case 0: SetViewCity1(); break;
            case 1: SetViewCity2(); break;
            case 2: SetViewCity3(); break;
            case 3: SetViewCity4(); break;
            case 4: SetViewCity5(); break;
            case 5: SetViewCity6(); break;
            case 6: SetViewCity7(); break;
            case 7: SetViewCity8(); break;
            default:
                Debug.LogWarning($"[CameraController] 잘못된 지역 인덱스입니다. 0~7 사이 입력값: {index}");
                break;
        }
    }

    public void SetViewByIndexFromString(string indexText)
    {
        if (!int.TryParse(indexText, out int index))
        {
            Debug.LogWarning($"[CameraController] 인덱스 파싱 실패: {indexText}");
            return;
        }
        SetViewByIndex(index);
    }

    private void ApplyView(Transform target, string label)
    {
        if (target == null || mainZoneCam == null)
        {
            Debug.LogWarning($"[CameraController] {label} 타겟이 없거나 메인 시네머신 캠이 연결되지 않았습니다.");
            return;
        }

        // ⭐ 프론트엔드에서 버튼을 눌러서 구역을 이동하면, 자유 모드는 강제로 끕니다!
        isFreeMode = false; 
        currentZoneView = target; // 나중에 스페이스바 껐을 때 돌아올 곳 기억하기

        mainZoneCam.transform.SetPositionAndRotation(target.position, target.rotation);
        baseRotation = target.rotation; 

        ReturnToMain();

        Debug.Log($"[CameraController] {label} 구역으로 시네머신 뷰 이동 완료!");
    }

    // ===================================================
    // 3. 도둑 줌인 / 줌아웃 (시네머신 전용)
    // ===================================================
    public void FocusOnTarget(Transform targetTransform)
    {
        if (closeUpCam == null) return;

        closeUpCam.Follow = targetTransform;
        closeUpCam.LookAt = targetTransform;
        closeUpCam.Priority = 20; 
        
        Debug.Log($"🎥 [CameraController] {targetTransform.name} 앞으로 줌인!");
    }

    public void ReturnToMain()
    {
        if (closeUpCam == null) return;

        closeUpCam.Follow = null;
        closeUpCam.LookAt = null;
        closeUpCam.Priority = closeUpDefaultPriority; 
        
        Debug.Log("🎥 [CameraController] 구역 메인 화면으로 복귀.");
    }
}