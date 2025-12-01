package com.myPhysioTime.application.dto

import com.myPhysioTime.domain.models.*
import java.time.LocalDateTime


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

fun TimeSlot.toDTO() = TimeSlotDTO(
    time = this.time,
    isAvailable = this.isAvailable
)

fun ServiceInfo.toDTO() = ServiceInfoDTO(
    id = this.id,
    name = this.name,
    price = this.price,
    duration = this.duration
)

fun ClientInfo.toDTO() = ClientInfoDTO(
    id = this.id,
    name = this.name,
    email = this.email,
    phone = this.phone
)

fun PhysiotherapeutInfo.toDTO() = PhysiotherapeutInfoDTO(
    id = this.id,
    name = this.name
)

fun CreateBookingRequest.toCommand(clientId: Int) = CreateBookingCommand(
    serviceId = this.serviceId,
    clientId = clientId,
    date = LocalDateTime.parse(this.date),
    notes = this.notes
)

fun UpdateBookingRequest.toCommand() = UpdateBookingCommand(
    state = this.state?.let { BookingState.valueOf(it.uppercase()) },
    notes = this.notes,
    physiotherapeutId = this.physiotherapeutId
)