package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PointServiceUnitTest {

    @Mock
    private lateinit var userPointTable: UserPointTable

    @Mock
    private lateinit var pointHistoryTable: PointHistoryTable

    @Mock
    private lateinit var pointValidator: PointValidator

    @InjectMocks
    private lateinit var pointService: PointService

    /**
     * 포인트 조회 정상 동작 테스트
     * stubbing을 통한 포인트 조회 로직의 상태 검증
     * UserPointTable의 메서드를 호출하여 반환하는 단순 로직인만큼 행위 검증도 추가
     */
    @Test
    fun `id를 통해 포인트를 조회한다`() {
        // given
        val userId = 1L
        val userPoint = UserPoint(userId, 1000L, 30L)
        `when`(userPointTable.selectById(userId)).thenReturn(userPoint)

        // when
        val actual = pointService.getUserPoint(userId)

        // then
        verify(userPointTable).selectById(1L)

        assertThat(actual.id).isEqualTo(1L)
        assertThat(actual.point).isEqualTo(1000L)
        assertThat(actual.updateMillis).isEqualTo(30L)
    }

    /**
     * 포인트 내역 조회 정상 동작 테스트
     * stubbing을 통한 포인트 조회 로직의 상태 검증
     * PointHistoryTable의 메서드를 호출하여 반환하는 단순 로직인만큼 행위 검증도 추가
     */
    @Test
    fun `id를 통해 포인트 내역을 조회한다`() {
        // given
        val userId = 11L
        val pointHistory1 = PointHistory(1L, userId, TransactionType.CHARGE, 1000L, 23L)
        val pointHistory2 = PointHistory(2L, userId, TransactionType.USE, 800L, 23L)
        `when`(pointService.getUserPointHistory(userId))
            .thenReturn(listOf(pointHistory1, pointHistory2))

        // when
        val actual = pointService.getUserPointHistory(userId)

        // then
        verify(pointHistoryTable).selectAllByUserId(11L)

        assertThat(actual).hasSize(2)

        assertThat(actual[0].id).isEqualTo(1L)
        assertThat(actual[0].userId).isEqualTo(11L)
        assertThat(actual[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(actual[0].amount).isEqualTo(1000L)

        assertThat(actual[1].id).isEqualTo(2L)
        assertThat(actual[1].userId).isEqualTo(11L)
        assertThat(actual[1].type).isEqualTo(TransactionType.USE)
        assertThat(actual[1].amount).isEqualTo(800L)
    }

    /**
     * 포인트 충전에 대한 정상 동작 단위 테스트
     * stubbing을 통해 충전 포인트에 대한 상태 검증
     * 행위 검증은 최소한으로 하여 로직상 필요한 로직이 호출되는지만 확인
     */
    @Test
    fun `id를 통해 amount만큼 포인트를 충전한다`() {
        // given
        val userId = 21L
        val existingPoint = 800L
        val chargeAmount = 300L

        val existingUserPoint = UserPoint(userId, existingPoint, 10L)

        `when`(userPointTable.selectById(userId)).thenReturn(existingUserPoint)
        `when`(userPointTable.insertOrUpdate(userId, existingPoint + chargeAmount))
            .thenAnswer { invocation -> UserPoint(invocation.getArgument(0), invocation.getArgument(1), 20L) }

        // when
        val actual = pointService.chargePoint(userId, chargeAmount)

        //then
        assertThat(actual.point).isEqualTo(1100L)

        verify(pointValidator).validateChargeable(800L, 300L)
        verify(pointHistoryTable).insert(userId, chargeAmount, TransactionType.CHARGE, 20L)
    }

    /**
     * 포인트 사용에 대한 정상 동작 단위 테스트
     * stubbing을 통해 충전 포인트에 대한 상태 검증
     * 행위 검증은 최소한으로 하여 로직상 필요한 로직이 호출되는지만 확인
     */
    @Test
    fun `id를 통해 amount만큼 포인트를 사용한다`() {
        // given
        val userId = 31L
        val existingPoint = 350L
        val useAmount = 280L

        val existingUserPoint = UserPoint(userId, existingPoint, 9L)

        `when`(userPointTable.selectById(userId)).thenReturn(existingUserPoint)
        `when`(userPointTable.insertOrUpdate(userId, existingPoint - useAmount))
            .thenAnswer { invocation -> UserPoint(invocation.getArgument(0), invocation.getArgument(1), 18L) }

        // when
        val actual = pointService.usePoint(userId, useAmount)

        //then
        assertThat(actual.point).isEqualTo(70L)

        verify(pointValidator).validateUseable(350L, 280L)
        verify(pointHistoryTable).insert(userId, useAmount, TransactionType.USE, 18L)
    }
}