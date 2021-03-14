package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }

    public User createUser(User newUser) {
        newUser.setToken(UUID.randomUUID().toString());
        newUser.setStatus(UserStatus.ONLINE);

        checkIfUserExists(newUser);

        // saves the given entity but data is only persisted in the database once flush() is called
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    //TODO: ask Joël or Dennis if this function is even neccessary because UserRepository should have findById right?
    public User findUserById(long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " User does not exist with this ID.");
        } else {
            return user;
        }
    }

    //TODO: edit-logic has to be implemented
    public void edit(long id, Date birthDate, String username) {
        User user = userRepository.findById(id);

    }

    //TODO: do el refactoring
    public User Login(User inputUser) {
        User user = userRepository.findByUsername(inputUser.getUsername());
        if(user != null) {
            if(user.getPassword().equals(inputUser.getPassword())) {
                user.setStatus(UserStatus.ONLINE);
                return user;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wrong user or password combination");
    }

    //TODO: im quite confused with this one. Logout would only be called if someone's logged in, so it does not make
    //      to make the check for the inputUser != null right?
    //TODO: Therefor the check for the token. If the token is still set, that means that it did not expire,
    //      hence the user is still logged in?!
    public User Logout(User inputUser) {
        if(inputUser.getToken() != null) {
            // is this line underneath redundant?
            inputUser.setToken(null);
            inputUser.setStatus(UserStatus.OFFLINE);
            return inputUser;
        }
        else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not logged in anymore. Maybe your login-token has expired or some other" +
                    " unforseeable shenanigans have occured such that this exception is thrown");
        }
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the username and the name
     * defined in the User entity. The method will do nothing if the input is unique and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        User userByName = userRepository.findByName(userToBeCreated.getName());

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
        if (userByUsername != null && userByName != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username and the name", "are"));
        }
        else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
        }
        else if (userByName != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
        }
    }
}
