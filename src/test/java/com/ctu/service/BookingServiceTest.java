package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Test Suite")
class BookingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BookingService bookingService;

    private Room availableRoom;

    @BeforeEach
    void setUp() {
        // ARRANGE: Dữ liệu dùng chung cho các test
        availableRoom = new Room("R101", 500000, true);
    }

    @Test
    @DisplayName("Đặt phòng thành công")
    void testBookRoom_Success() {
        // ARRANGE
        when(roomRepository.findById("R101")).thenReturn(Optional.of(availableRoom));

        when(paymentGateway.charge("CARD001", 1000000)).thenReturn(true);

        // ACT
        BookingReceipt receipt = bookingService.bookRoom("R101", 2, "CARD001");

        // ASSERT
        assertNotNull(receipt);
        assertNotNull(receipt.getBookingId());
        assertFalse(availableRoom.isAvailable());
        assertEquals("R101", receipt.getRoomId());
        assertEquals(1000000, receipt.getTotalAmount(), 0.001);

        verify(roomRepository, times(1)).findById("R101");
        verify(paymentGateway, times(1)).charge("CARD001", 1000000);
        verify(roomRepository, times(1)).save(availableRoom);
    }

    @Test
    @DisplayName("Không tìm thấy phòng")
    void testBookRoom_RoomNotFound_ThrowsException() {
        // ARRANGE
        when(roomRepository.findById("R999")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RoomNotAvailableException.class, () -> bookingService.bookRoom("R999", 1, "CARD001"));
        verify(roomRepository, times(1)).findById("R999");
        verifyNoInteractions(paymentGateway);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Phòng đã được đặt trước")
    void testBookRoom_RoomAlreadyBooked_ThrowsException() {
        // ARRANGE
        Room bookedRoom = new Room("R102", 500000, false);

        when(roomRepository.findById("R102")).thenReturn(Optional.of(bookedRoom));

        // ACT & ASSERT
        assertThrows(RoomNotAvailableException.class,() -> bookingService.bookRoom("R102", 1, "CARD001"));
        verifyNoInteractions(paymentGateway);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Thanh toán thất bại")
    void testBookRoom_PaymentFailed_ThrowsException() {
        // ARRANGE
        when(roomRepository.findById("R101")).thenReturn(Optional.of(availableRoom));
        when(paymentGateway.charge("CARD001", 500000)).thenReturn(false);

        // ACT & ASSERT
        assertThrows(PaymentFailedException.class, () -> bookingService.bookRoom("R101", 1, "CARD001"));

        // Phòng vẫn phải còn trống nếu thanh toán thất bại
        assertTrue(availableRoom.isAvailable());
        verify(paymentGateway, times(1)).charge("CARD001", 500000);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Số đêm bằng 0 không hợp lệ")
    void testBookRoom_InvalidNights_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.bookRoom("R101", 0, "CARD001"));
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Số đêm âm không hợp lệ")
    void testBookRoom_NegativeNights_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.bookRoom("R101", -1, "CARD001"));
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Thẻ tín dụng null không hợp lệ")
    void testBookRoom_NullCreditCard_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.bookRoom("R101", 1, null));
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Thẻ tín dụng rỗng không hợp lệ")
    void testBookRoom_EmptyCreditCard_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.bookRoom("R101", 1, ""));
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Mã phòng null không hợp lệ")
    void testBookRoom_NullRoomId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.bookRoom(null, 1, "CARD001"));
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Thẻ tín dụng chỉ chứa khoảng trắng")
    void testBookRoom_BlankCreditCard_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.bookRoom("R101", 1, "   "));
        verifyNoInteractions(roomRepository, paymentGateway);
    }
}