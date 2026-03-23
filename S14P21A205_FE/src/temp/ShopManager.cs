using UnityEngine;
using UnityEngine.Serialization; // [FormerlySerializedAs]를 사용하기 위해 필수!

public class ShopManager : MonoBehaviour // 유니티 컴포넌트로 쓰려면 MonoBehaviour 필수!
{
    [Header("소환할 Shop 프리팹")]
    public GameObject shopPrefab;

    [Header("기본 소환 위치 (8개)")]
    [FormerlySerializedAs("spawnPoints")]
    public Transform[] basePoints;

    // 💡 [핵심] 현재 맵에 소환되어 있는 상점을 기억하는 '추적용 변수'
    private GameObject currentShopInstance;

    /// <summary>
    /// 1. 특정 인덱스 위치에 상점을 소환하는 함수 (기존 상점 자동 삭제)
    /// </summary>
    /// <param name="index">소환할 위치의 인덱스 (예: 0 ~ 7)</param>
    public void SpawnShopAtIndex(int index)
    {
        // 안전장치: 인덱스가 배열 범위를 벗어나면 에러 띄우고 중단!
        if (index < 0 || index >= basePoints.Length)
        {
            Debug.LogWarning($"[ShopManager] 잘못된 위치 인덱스입니다! 0부터 {basePoints.Length - 1} 사이의 값을 넣어주세요.");
            return;
        }

        // 새로운 상점을 깔기 전에, 기존에 깔려있는 상점이 있다면 먼저 청소!
        ClearAllShops();

        // 선택한 인덱스의 위치(Position)와 회전(Rotation) 값을 가져옴
        Transform targetPoint = basePoints[index];

        // 해당 위치에 프리팹을 소환하고, currentShopInstance 변수에 기억시킴
        currentShopInstance = Instantiate(shopPrefab, targetPoint.position, targetPoint.rotation);
        
        Debug.Log($"[ShopManager] {index}번 위치에 상점 소환 완료!");
    }

    /// <summary>
    /// 2. 맵에 있는 상점을 싹 지워주는 함수
    /// </summary>
    public void ClearAllShops()
    {
        // 맵에 기억하고 있는 상점이 존재한다면?
        if (currentShopInstance != null)
        {
            Destroy(currentShopInstance); // 파괴!
            currentShopInstance = null;   // 기억 삭제 (빈손으로 만들기)
            Debug.Log("[ShopManager] 기존 상점 철거 완료!");
        }
    }
    // 💡 [테스트용 코드] 유니티 끄기 전에 나중에 꼭 지우거나 주석 처리하세요!
    private void Update()
    {
        // 키보드 숫자 1번을 누르면 -> 0번 인덱스 위치에 소환
        if (Input.GetKeyDown(KeyCode.Alpha1))
        {
            SpawnShopAtIndex(0);
        }
        // 키보드 숫자 2번을 누르면 -> 1번 인덱스 위치에 소환
        if (Input.GetKeyDown(KeyCode.Alpha2))
        {
            SpawnShopAtIndex(1);
        }
        // 키보드 숫자 3번을 누르면 -> 2번 인덱스 위치에 소환
        if (Input.GetKeyDown(KeyCode.Alpha3))
        {
            SpawnShopAtIndex(2);
        }

        // 스페이스바를 누르면 -> 맵에 있는 상점 싹 철거!
        if (Input.GetKeyDown(KeyCode.Space))
        {
            ClearAllShops();
        }
    }
}