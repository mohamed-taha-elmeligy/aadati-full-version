package com.mts.aadati.services;

import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitDayWeek;
import com.mts.aadati.exceptions.exception.DuplicateResourceException;
import com.mts.aadati.exceptions.exception.InvalidRequestException;
import com.mts.aadati.repository.HabitDayWeekRepository;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitDayWeekService {

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String DAY_ALREADY_EXISTS = "DayWeek already exists";
    private static final String CAN_NOT_BE_NULL = "can't be null";

    private final HabitDayWeekRepository repository;

    @Transactional
    public HabitDayWeek addDay(HabitDayWeek dayWeek){
        if (repository.existsByDayOfWeek(dayWeek.getDayOfWeek()))
            throw new DuplicateResourceException(DAY_ALREADY_EXISTS);

        return repository.save(dayWeek);
    }

    public List<HabitDayWeek> findByDayAsc(){
        return repository.findByOrderByDayOfWeekAsc();
    }

    public Page<Habit> findHabitsByDayAndUser(
            DayOfWeek day, UUID userId, int pageNumber
    ){
        if (day == null)
            throw new InvalidRequestException("DayOfWeek " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.findHabitsByDayOfWeekAndUserId(
                day,
                userId,
                PageableUtils.pageable(pageNumber,SORT_CREATED_AT)
        );
    }

    public long countHabitsByDayAndUser(DayOfWeek day, UUID userId){
        if (day == null)
            throw new InvalidRequestException("DayOfWeek " + CAN_NOT_BE_NULL);
        if (userId == null)
            throw new InvalidRequestException("User Id " + CAN_NOT_BE_NULL);

        return repository.countHabitsByDayOfWeekAndUserId(day, userId);
    }

}