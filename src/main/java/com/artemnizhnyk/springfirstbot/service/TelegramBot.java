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
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
    public static final String YES_BUTTON = "YES_BUTTON";
    public static final String NO_BUTTON = "NO_BUTTON";
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

            if (messageText.contains("/send") && config.getOwnerId() == chatId) {
                String textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                Iterable<User> users = userRepository.findAll();
                users.forEach(user -> sendMessage(user.getChatId(), textToSend));
                return;
            }

            switch (messageText) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                case "/register" -> register(chatId);
                default -> sendMessage(chatId, "Sorry, command wasn't recognized");
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            long messageId = callbackQuery.getMessage().getMessageId();
            long chatId = callbackQuery.getMessage().getChatId();
            String callbackData = callbackQuery.getData();

            String text;
            if (callbackData.equals(YES_BUTTON)) {
                text = "You pressed YES button";
                executeEditedMessageText(chatId, text, messageId);
            } else if (callbackData.equals(NO_BUTTON)) {
                text = "You pressed NO button";
                executeEditedMessageText(chatId, text, messageId);
            }
        }
    }

    private void executeEditedMessageText(final long chatId, final String text, long messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setText(text);
        editMessageText.setMessageId((int)messageId);
        executeMessage(editMessageText);
    }

    private void register(final long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInline.add(yesButton);
        rowInline.add(noButton);

        rowsInline.add(rowInline);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
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

        executeMessage(message);
    }

    private void executeMessage(final Object message) {
        try {
            if (message.equals(SendMessage.class)) {
                execute((SendMessage) message);
            } else if (message.equals(SendVoice.class)) {
                execute((SendVoice) message);
            } else if (message.equals(EditMessageText.class)) {
                execute((EditMessageText) message);
            }
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendAudioMessage(final long chatId) {
        SendVoice message = new SendVoice(String.valueOf(chatId), new InputFile(new File("src/main/resources/audio/startaudio.m4a"), "hiMessage"));
        executeMessage(message);
    }
}
