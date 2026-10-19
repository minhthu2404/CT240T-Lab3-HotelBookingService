package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;

import java.util.UUID;

public class BookingService {

    private final RoomRepository roomRepository;
    private final PaymentGateway paymentGateway;

    public BookingService(RoomRepository roomRepository, PaymentGateway paymentGateway) {
        this.roomRepository = roomRepository;
        this.paymentGateway = paymentGateway;
    }

    public BookingReceipt bookRoom(String roomId, int nights, String creditCard) {
        // 1. Kiểm tra tham số đầu vào
        if (roomId == null || nights <= 0 || creditCard == null || creditCard.trim().isEmpty()) {
            throw new IllegalArgumentException("Tham so dat phong khong hop le!");
        }

        // 2. Tìm phòng
        Room room = roomRepository.findById(roomId).orElseThrow(() 
        		-> new RoomNotAvailableException("Khong tim thay phong: " + roomId));

        // 3. Kiểm tra phòng còn trống
        if (!room.isAvailable()) {
            throw new RoomNotAvailableException("Phong da duoc dat truoc do: " + roomId);
        }

        // 4. Tính tiền
        double totalAmount = room.getPricePerNight() * nights;

        // 5. Thanh toán
        boolean paymentSuccess = paymentGateway.charge(creditCard, totalAmount);

        if (!paymentSuccess) {
            throw new PaymentFailedException("Thanh toan qua the credit card that bai!");
        }

        // 6. Cập nhật trạng thái và lưu phòng
        room.setAvailable(false);
        roomRepository.save(room);

        // 7. Trả về biên nhận
        return new BookingReceipt(UUID.randomUUID().toString(), roomId, totalAmount);
    }
}
