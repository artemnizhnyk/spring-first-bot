package com.artemnizhnyk.springfirstbot.service;

import com.artemnizhnyk.springfirstbot.config.BotConfig;
import com.artemnizhnyk.springfirstbot.entity.User;
import com.artemnizhnyk.springfirstbot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String HELP_TEXT = """
            This bot is created to demonstrate Artem Nizhnyk
            super mega high coding skill and his knowledge)))
                        
            - type /start to see a welcome message
                        
            - type /mydata to see stored data about yourself
                        
            - type /deletedata to delete stored data about yourself
                        
            - type /settings to configure your preferences
             
            P.S. You are LOX !!!
            """;
    @Autowired
    private UserRepository userRepository;
    private final BotConfig config;

    public TelegramBot(final BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletemydata", "delete your data stored"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(final Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                default -> sendMessage(chatId, "Sorry, command wasn't recognized");
            }
        }
    }

    private void registerUser(Message message) {
        if (!userRepository.existsById(message.getChatId())) {

            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setFirstname(chat.getFirstName());
            newUser.setLastname(chat.getLastName());
            newUser.setUsername(chat.getUserName());
            newUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(newUser);
            log.info("User saved: " + newUser);
        }
    }

    private void startCommandReceived(final long chatId, final String firstName) {
        String answer = EmojiParser.parseToUnicode(firstName + ", idi nahui" + ":alien:");
        log.info("Replied to user " + firstName);
        sendAudioMessage(chatId);
        sendMessage(chatId, answer);
    }

    private void sendMessage(final long chatId, final String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendAudioMessage(final long chatId) {
        SendVoice message = new SendVoice(String.valueOf(chatId), new InputFile(new File("src/main/resources/audio/startaudio.m4a"), "hiMessage"));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
