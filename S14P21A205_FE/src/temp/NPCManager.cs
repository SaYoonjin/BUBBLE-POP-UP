using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Serialization;

public class NPCManager : MonoBehaviour
{
    public static NPCManager Instance;

    [Header("소환할 NPC 뼈대 프리팹")]
    public GameObject baseNpcPrefab;

    [Header("오브젝트 풀링 설정")]
    [Tooltip("게임 시작 시 생성할 최대 NPC 수 (이 이상 절대 생성되지 않음!)")]
    public int initialPoolSize = 100; // 대장님 요청대로 기본값을 100으로 뒀습니다!

    [Header("기본 이동 위치 (8개)")]
    [FormerlySerializedAs("spawnPoints")]
    public Transform[] basePoints;

    [Header("팝업스토어 위치 (8개)")]
    [FormerlySerializedAs("destinationPoints")]
    public Transform[] popupStorePoints;

    [Header("상시 혼잡도 스폰")]
    [Range(1, 5)]
    public int defaultCongestionLevel = 2;
    [Tooltip("혼잡도 단계당 2초마다 N명 소환합니다.")]
    public float ambientSpawnTickSeconds = 2f;

    [Header("팝업스토어 방문 설정")]
    public float popupStaySeconds = 2f;
    public float popupBatchWindowSeconds = 10f;

    private readonly Queue<GameObject> npcPool = new Queue<GameObject>();
    private Coroutine ambientSpawnCoroutine;
    private int currentCongestionLevel;

    void Awake()
    {
        Instance = this;
        InitializePool();
    }

    void Start()
    {
        SetCongestionLevel(defaultCongestionLevel);
        StartAmbientSpawning();
    }

    void OnDisable()
    {
        StopAmbientSpawning();
    }

    private void InitializePool()
    {
        if (baseNpcPrefab == null)
        {
            Debug.LogWarning("[NPCManager] baseNpcPrefab이 비어 있어 풀을 초기화하지 못했습니다.");
            return;
        }

        for (int i = 0; i < initialPoolSize; i++)
        {
            GameObject npc = Instantiate(baseNpcPrefab, transform);
            npc.SetActive(false);
            npcPool.Enqueue(npc);
        }
    }

    public GameObject GetNPCFromPool(Vector3 position, Quaternion rotation)
    {
        // ⭐ 핵심 1: 풀에 남은 애가 있으면 꺼내주고, 없으면 새로 만들지 않고 null을 던집니다!
        if (npcPool.Count > 0)
        {
            GameObject npc = npcPool.Dequeue();
            npc.transform.position = position;
            npc.transform.rotation = rotation;
            npc.SetActive(true);
            return npc;
        }
        else
        {
            // Debug.Log("[NPCManager] 100명 한도 초과! 더 이상 생성하지 않고 대기합니다.");
            return null; 
        }
    }

    public void ReturnNPCToPool(GameObject npc)
    {
        if (npc == null)
        {
            return;
        }

        npc.SetActive(false);
        npcPool.Enqueue(npc);
    }

    public void StartAmbientSpawning()
    {
        if (ambientSpawnCoroutine != null)
        {
            StopCoroutine(ambientSpawnCoroutine);
        }

        ambientSpawnCoroutine = StartCoroutine(AmbientSpawnLoop());
    }

    public void StopAmbientSpawning()
    {
        if (ambientSpawnCoroutine == null)
        {
            return;
        }

        StopCoroutine(ambientSpawnCoroutine);
        ambientSpawnCoroutine = null;
    }

    public void SetCongestionLevel(int level)
    {
        currentCongestionLevel = Mathf.Clamp(level, 1, 5);
        Debug.Log($"[NPCManager] 혼잡도 단계 변경: {currentCongestionLevel}");

        if (ambientSpawnCoroutine == null && isActiveAndEnabled)
        {
            ambientSpawnCoroutine = StartCoroutine(AmbientSpawnLoop());
        }
    }

    public void SpawnPopupVisitors(int popupStoreIndex, int count)
    {
        if (!CanSpawnPopupVisitor(popupStoreIndex))
        {
            return;
        }

        if (count <= 0)
        {
            Debug.LogWarning($"[NPCManager] 팝업스토어 방문 인원은 1명 이상이어야 합니다. 입력값: {count}");
            return;
        }

        StartCoroutine(SpawnPopupVisitorsRoutine(popupStoreIndex, count));
    }

    public void SpawnPopupVisitorsFromPayload(string payload)
    {
        if (string.IsNullOrWhiteSpace(payload))
        {
            Debug.LogWarning("[NPCManager] SpawnPopupVisitorsFromPayload payload가 비어 있습니다.");
            return;
        }

        string[] parts = payload.Split(',');
        if (parts.Length != 2)
        {
            Debug.LogWarning($"[NPCManager] payload 형식이 잘못되었습니다. 기대 형식: popupStoreIndex,count / 실제값: {payload}");
            return;
        }

        if (!int.TryParse(parts[0], out int popupStoreIndex) || !int.TryParse(parts[1], out int count))
        {
            Debug.LogWarning($"[NPCManager] payload 파싱에 실패했습니다. 실제값: {payload}");
            return;
        }

        SpawnPopupVisitors(popupStoreIndex, count);
    }

    public void SpawnGuestToDestination(int popupStoreIndex)
    {
        SpawnPopupVisitors(popupStoreIndex, 1);
    }

    [ContextMenu("테스트: 상시 경로 NPC 1명")]
    public void TestSpawnAmbientNpc()
    {
        SpawnAmbientNpc();
    }

    [ContextMenu("테스트: 팝업스토어 0 방문 NPC 1명")]
    public void TestSpawnPopupNpc()
    {
        SpawnPopupVisitors(0, 1);
    }

    private IEnumerator AmbientSpawnLoop()
    {
        WaitForSeconds wait = new WaitForSeconds(Mathf.Max(0.1f, ambientSpawnTickSeconds));

        while (true)
        {
            if (CanSpawnAmbientNpc())
            {
                for (int i = 0; i < currentCongestionLevel; i++)
                {
                    SpawnAmbientNpc();
                }
            }

            yield return wait;
        }
    }

    private IEnumerator SpawnPopupVisitorsRoutine(int popupStoreIndex, int count)
    {
        float interval = count > 0 ? popupBatchWindowSeconds / count : popupBatchWindowSeconds;
        WaitForSeconds wait = interval > 0f ? new WaitForSeconds(interval) : null;

        for (int i = 0; i < count; i++)
        {
            SpawnPopupVisitor(popupStoreIndex);

            if (i < count - 1 && wait != null)
            {
                yield return wait;
            }
        }
    }

    private void SpawnAmbientNpc()
    {
        if (!TryGetRandomBaseRoute(out Transform startTransform, out Transform destinationTransform))
        {
            return;
        }

        GameObject npc = GetNPCFromPool(startTransform.position, startTransform.rotation);
        
        // ⭐ 핵심 2: 만약 GetNPCFromPool이 null을 반환했다면(100명이 꽉 찼다면) 그냥 여기서 함수 종료!
        if (npc == null) return; 

        NPCMovement movement = npc.GetComponent<NPCMovement>();

        if (movement == null)
        {
            Debug.LogWarning("[NPCManager] NPCMovement가 없어 NPC를 풀로 복귀시킵니다.");
            ReturnNPCToPool(npc);
            return;
        }

        movement.BeginAmbientRoute(destinationTransform.position);
    }

    private void SpawnPopupVisitor(int popupStoreIndex)
    {
        if (!CanSpawnPopupVisitor(popupStoreIndex))
        {
            return;
        }

        if (!TryGetRandomBaseRoute(out Transform startTransform, out Transform finalBaseTransform))
        {
            return;
        }

        Transform popupStoreTransform = popupStorePoints[popupStoreIndex];
        GameObject npc = GetNPCFromPool(startTransform.position, startTransform.rotation);
        
        // ⭐ 핵심 2: 풀이 비었으면 새로 뽑지 않고 취소!
        if (npc == null) return; 

        NPCMovement movement = npc.GetComponent<NPCMovement>();

        if (movement == null)
        {
            Debug.LogWarning("[NPCManager] NPCMovement가 없어 NPC를 풀로 복귀시킵니다.");
            ReturnNPCToPool(npc);
            return;
        }

        movement.BeginPopupRoute(
            popupStoreTransform.position,
            finalBaseTransform.position,
            popupStaySeconds,
            NPCReactBridge.SendPopupArrivalSignal
        );
    }

    private bool CanSpawnAmbientNpc()
    {
        if (baseNpcPrefab == null)
        {
            Debug.LogWarning("[NPCManager] baseNpcPrefab이 비어 있어 상시 NPC를 소환할 수 없습니다.");
            return false;
        }

        if (basePoints == null || basePoints.Length < 2)
        {
            Debug.LogWarning("[NPCManager] basePoints는 최소 2개 이상 필요합니다.");
            return false;
        }

        return true;
    }

    private bool CanSpawnPopupVisitor(int popupStoreIndex)
    {
        if (!CanSpawnAmbientNpc())
        {
            return false;
        }

        if (popupStorePoints == null || popupStorePoints.Length == 0)
        {
            Debug.LogWarning("[NPCManager] popupStorePoints가 비어 있어 팝업 방문 NPC를 소환할 수 없습니다.");
            return false;
        }

        if (popupStoreIndex < 0 || popupStoreIndex >= popupStorePoints.Length)
        {
            Debug.LogWarning($"[NPCManager] popupStoreIndex 범위를 벗어났습니다. 입력값: {popupStoreIndex}");
            return false;
        }

        return true;
    }

    private bool TryGetRandomBaseRoute(out Transform startTransform, out Transform destinationTransform)
    {
        startTransform = null;
        destinationTransform = null;

        if (!CanSpawnAmbientNpc())
        {
            return false;
        }

        int startIndex = Random.Range(0, basePoints.Length);
        int destinationIndex = GetRandomBaseDestinationIndex(startIndex);

        startTransform = basePoints[startIndex];
        destinationTransform = basePoints[destinationIndex];
        return startTransform != null && destinationTransform != null;
    }

    private int GetRandomBaseDestinationIndex(int excludedIndex)
    {
        int destinationIndex = Random.Range(0, basePoints.Length - 1);
        return destinationIndex >= excludedIndex ? destinationIndex + 1 : destinationIndex;
    }
}