package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class PlayerServiceImpl implements PlayerService{

    private PlayerRepository playerRepository;

    public PlayerServiceImpl() {

    }

    @Autowired
    public PlayerServiceImpl(PlayerRepository playerRepository) {
        super();
        this.playerRepository = playerRepository;
    }
    @Override
    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    @Override
    public Player getPlayer(Long id) {
        return playerRepository.findById(id).orElse(null);
    }

    @Override
    public Player updatePlayer(Player oldPlayer, Player newPlayer) {

        String name = newPlayer.getName();
        if (name != null) {
            if (isNameValid(name)) {
                oldPlayer.setName(newPlayer.getName());
            } else {
                throw new IllegalArgumentException();
            }
        }

        String title = newPlayer.getTitle();
        if (title != null) {
            if (isTitleValid(title)) {
                oldPlayer.setTitle(newPlayer.getTitle());
            } else {
                throw new IllegalArgumentException();
            }
        }

        Race race = newPlayer.getRace();
        if (race != null) {
            oldPlayer.setRace(race);
        }

        Profession profession = newPlayer.getProfession();
        if (profession != null) {
            oldPlayer.setProfession(profession);
        }

        Integer experience = newPlayer.getExperience();
        if (experience != null) {
            if (isExperienceValid(experience)) {
                oldPlayer.setExperience(experience);
                Integer level = computeLevel(experience);
                oldPlayer.setLevel(level);
                oldPlayer.setUntilNextLevel(computeExperienceNextLevel(level, experience));
            } else {
                throw new IllegalArgumentException();
            }
        }

        Date birthday = newPlayer.getBirthday();
        if (birthday != null) {
            if (isBirthdayValid(birthday)) {
                oldPlayer.setBirthday(newPlayer.getBirthday());
            } else {
                throw new IllegalArgumentException();
            }
        }

        Boolean banned = newPlayer.getBanned();
        if (banned != null) {
            oldPlayer.setBanned(banned);
        }

        playerRepository.save(oldPlayer);
        return oldPlayer;
    }

    @Override
    public void deletePlayer(Player player) {
        playerRepository.delete(player);
    }

    @Override
    public List<Player> getPlayers(
            String name,
            String title,
            Race race,
            Profession profession,
            Long after,
            Long before,
            Boolean banned,
            Integer minExperience,
            Integer maxExperience,
            Integer minLevel,
            Integer maxLevel
    ) {
        final Date afterDate = after == null ? null : new Date(after);
        final Date beforeDate = before == null ? null : new Date(before);
        final List<Player> list = new ArrayList<>();
        playerRepository.findAll().forEach(player -> {
            if (name != null && !player.getName().contains(name)) return;
            if (title != null && !player.getTitle().contains(title)) return;
            if (race != null && player.getRace() != race) return;
            if (profession != null && player.getProfession() != profession) return;
            if (afterDate != null && player.getBirthday().before(afterDate)) return;
            if (beforeDate != null && player.getBirthday().after(beforeDate)) return;
            if (banned != null && player.getBanned().booleanValue() != banned.booleanValue()) return;
            if (minExperience != null && player.getExperience().compareTo(minExperience) < 0) return;
            if (maxExperience != null && player.getExperience().compareTo(maxExperience) > 0) return;
            if (minLevel != null && player.getLevel().compareTo(minLevel) < 0) return;
            if (maxLevel != null && player.getLevel().compareTo(maxLevel) > 0) return;

            list.add(player);
        });
        return list;
    }

    @Override
    public List<Player> sortPlayers(List<Player> players, PlayerOrder order) {
        if (order != null) {
            players.sort((player1, player2) -> {
                switch (order) {
                    case ID:
                        return player1.getId().compareTo(player2.getId());
                    case NAME:
                        return player1.getName().compareTo(player2.getName());
                    case EXPERIENCE:
                        return player1.getExperience().compareTo(player2.getExperience());
                    case BIRTHDAY:
                        return player1.getBirthday().compareTo(player2.getBirthday());
                    case LEVEL:
                        return player1.getLevel().compareTo(player2.getLevel());
                    default:
                        return 0;
                }
            });
        }
        return players;
    }

    @Override
    public List<Player> getPage(List<Player> players, Integer pageNumber, Integer pageSize) {
        final int page = pageNumber == null ? 0 : pageNumber;
        final int size = pageSize == null ? 3 : pageSize;
        final int from = page * size;
        int to = from + size;
        if (to > players.size()) {
            to = players.size();
        }
        return players.subList(from,to);
    }

    @Override
    public boolean isPlayerValid(Player player) {
        return player != null &&
                isNameValid(player.getName()) &&
                isTitleValid(player.getTitle()) &&
                isExperienceValid(player.getExperience()) &&
                isBirthdayValid(player.getBirthday());
    }

    public boolean isNameValid(String name) {
        if (name != null) {
            int nameLength = name.length();
            return nameLength <= 12 && !name.isEmpty();
        } else {
            return false;
        }
    }

    public boolean isTitleValid(String title) {
        int titleLength = title.length();
        return titleLength <= 30;
    }

    public boolean isExperienceValid(Integer experience) {
        return experience != null && experience > 0 && experience < 10000000;
    }

    public boolean isBirthdayValid(Date date) {
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.YEAR, 2000);
        final Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(Calendar.YEAR, 3000);
        Date start = startCalendar.getTime();
        Date end = endCalendar.getTime();
        return date != null && date.after(start) && date.before(end) && date.getTime() > 0;
    }

    @Override
    public Integer computeLevel(Integer exp) {
        return (int) ((Math.sqrt(2500 + 200 * exp)) - 50) / 100;
    }

    @Override
    public Integer computeExperienceNextLevel(Integer level, Integer exp) {
        return 50 * (level + 1) * (level + 2) - exp;
    }
}
