package com.scaler.assignment.services;

import com.scaler.assignment.dtos.requestdtos.CreateInterviewRequestDto;
import com.scaler.assignment.dtos.requestdtos.DeleteInterviewRequestDto;
import com.scaler.assignment.dtos.requestdtos.UpdateInterviewRequestDto;
import com.scaler.assignment.models.Interview;
import com.scaler.assignment.models.User;
import com.scaler.assignment.repository.InterviewRepository;
import com.scaler.assignment.repository.UserRepository;
import com.scaler.assignment.exceptions.ConflictOfTimingException;
import com.scaler.assignment.exceptions.InterviewNotFoundException;
import com.scaler.assignment.exceptions.InvalidDatesException;
import com.scaler.assignment.exceptions.UserDoesNotExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class InterviewService {
    private InterviewRepository interviewRepository;
    private UserRepository userRepository;

    @Autowired
    public InterviewService (InterviewRepository interviewRepository, UserRepository userRepository)   {
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    public List<Interview> getListOfInterview()  {

        List<Interview> interviewList = interviewRepository.findAll();
        return interviewList;
    }

    public Interview createInterview (CreateInterviewRequestDto requestDto) throws Exception {

        System.out.println("debug 1+1+1");

        Long requestingUserId = requestDto.getRequestedById();
        Long requestedUserId = requestDto.getRequestedToId();



        System.out.println("first user id" + requestingUserId);
        System.out.println("second user id" + requestedUserId);

        Optional<User> userRequesting = userRepository.findById(requestingUserId);
        Optional<User> userRequested = userRepository.findById(requestedUserId);

        if(userRequesting.isEmpty() || userRequested.isEmpty()) {
            throw new UserDoesNotExistException("Invalid Id/ User does not exist");
        }

//        System.out.println("debug 9+9+9");

        User requestingUser = userRequesting.get();
        User requestedUser = userRequested.get();

        LocalDateTime interviewStartTime = requestDto.getStartTime();
        LocalDateTime interviewEndTime = requestDto.getEndTime();

        if(interviewStartTime.compareTo(interviewEndTime) >= 0 ||
                interviewStartTime.compareTo(LocalDateTime.now()) < 0) {
            throw new InvalidDatesException ("Dates provided are invalid");
        }

        List<Interview> interviewsOfRequestingUser = interviewRepository.findByBookedBy(requestingUser.getId(),
                interviewStartTime,
                interviewEndTime);
        List<Interview> interviewsOfRequestedUser = interviewRepository.findByBookedBy(requestedUser.getId(),
                interviewStartTime,
                interviewEndTime);

        System.out.println(interviewsOfRequestingUser.size()+" "+interviewsOfRequestedUser.size());

        if(interviewsOfRequestingUser.isEmpty() && interviewsOfRequestedUser.isEmpty()) {

            Interview interview = new Interview();
            setInterview (interview, interviewStartTime, interviewEndTime, requestingUser, requestedUser);

            return interview;
        }

        throw new ConflictOfTimingException("One of user has already scheduled an interview at requested time interval. ");
    }

    private Interview setInterview (Interview interview, LocalDateTime startTime, LocalDateTime endTime,
                                    User requestedBy, User requestedWith)    {

        interview.setStartTime(startTime);
        interview.setEndTime(endTime);
        interview.setBookedBy(requestedBy);
        interview.setBookedWith(requestedWith);
        interviewRepository.save(interview);

        return interview;
    }

    public Interview updateInterview (UpdateInterviewRequestDto requestDto, long id) throws Exception {

        Optional<Interview> interview = interviewRepository.findById(id);

        if(interview.isEmpty()) {
            throw new InterviewNotFoundException("Requested interview does not exit");
        }

        Interview scheduledInterview = interview.get();

        Optional<User> user = userRepository.findById(requestDto.getBookWithId());

        if(user.isEmpty())  {
            throw new UserDoesNotExistException("Requested user does not exist");
        }

        User bookWithUser = user.get();
        User requestingUser = scheduledInterview.getBookedBy();

        LocalDateTime start = requestDto.getNewStartTime();
        LocalDateTime end = requestDto.getNewEndTime();

        if(start.compareTo(end) >= 0 ||
                start.compareTo(LocalDateTime.now()) < 0) {
            throw new InvalidDatesException ("Dates provided are invalid");
        }

        List<Interview> scheduledInterviewsOfRequestingUser = interviewRepository.findByParams(bookWithUser.getId(),
                start,
                end,
                id);

        List<Interview> scheduledInterviewsOfRequestedUsers = interviewRepository.findByParams(requestingUser.getId(),
                start,
                end,
                id);

        if(scheduledInterviewsOfRequestedUsers.isEmpty() &&
            scheduledInterviewsOfRequestingUser.isEmpty())  {

            setInterview(scheduledInterview, start, end, requestingUser, bookWithUser);

            return scheduledInterview;
        }

        throw new ConflictOfTimingException ("One of user has already scheduled an interview at requested time interval. ");
    }

    public Interview deleteInterview (DeleteInterviewRequestDto requestDto, long id) {
        return null;
    }
}
