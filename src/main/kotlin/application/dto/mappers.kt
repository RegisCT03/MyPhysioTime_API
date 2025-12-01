package com.myPhysioTime.application.dto

import com.myPhysioTime.domain.models.*
import java.time.LocalDateTime

// BookingDetail → BookingDTO
fun BookingDetail.toDTO() = BookingDTO(
    id = this.id,
    service = this.service.toDTO(),
    client = this.client.toDTO(),
    physiotherapeut = this.physiotherapeut?.toDTO(),
    date = this.date.toString(),
    state = this.state.name,
    notes = this.notes,
    createdAt = this.createdAt.toString()
)

// Booking → BookingDTO (por si usas Booking en create/update)
fun Booking.toDTO() = BookingDTO(
    id = this.id,
    service = ServiceInfoDTO(id = this.serviceId, name = "", price = 0.0, duration = 0), // placeholder
    client = ClientInfoDTO(id = this.clientId, name = "", email = "", phone = ""),       // placeholder
    physiotherapeut = this.physiotherapeutId?.let { PhysiotherapeutInfoDTO(id = it, name = "") },
    date = this.date.toString(),
    state = this.state.name,
    notes = this.notes,
    createdAt = this.createdAt.toString()
)

// TimeSlot → TimeSlotDTO
fun TimeSlot.toDTO() = TimeSlotDTO(
    time = this.time,
    isAvailable = this.isAvailable
)

// ServiceInfo → ServiceInfoDTO
fun ServiceInfo.toDTO() = ServiceInfoDTO(
    id = this.id,
    name = this.name,
    price = this.price,
    duration = this.duration
)

// ClientInfo → ClientInfoDTO
fun ClientInfo.toDTO() = ClientInfoDTO(
    id = this.id,
    name = this.name,
    email = this.email,
    phone = this.phone
)

// PhysiotherapeutInfo → PhysiotherapeutInfoDTO
fun PhysiotherapeutInfo.toDTO() = PhysiotherapeutInfoDTO(
    id = this.id,
    name = this.name
)

// CreateBookingRequest → CreateBookingCommand
fun CreateBookingRequest.toCommand(clientId: Int) = CreateBookingCommand(
    serviceId = this.serviceId,
    clientId = clientId,
    date = LocalDateTime.parse(this.date),
    notes = this.notes
)

// UpdateBookingRequest → UpdateBookingCommand
fun UpdateBookingRequest.toCommand() = UpdateBookingCommand(
    state = this.state?.let { BookingState.valueOf(it.uppercase()) },
    notes = this.notes,
    physiotherapeutId = this.physiotherapeutId
)