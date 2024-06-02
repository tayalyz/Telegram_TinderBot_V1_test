package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "";  //TODO telegram bot nickname
    public static final String TELEGRAM_BOT_TOKEN = "";  //TODO telegram bot token
    public static final String OPEN_AI_TOKEN = "";  //TODO gpt token
    private final ChatGPTService chatGPTService = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private final ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo her;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);
            sendTextMessage("Привет! Это хрю хрю бот");

            showMainMenu("главное меню бота", "/start",
                    "генерация Tinder-профля", "/profile",
                    "сообщение для знакомства", "/opener",
                    "переписка от вашего имени", "/message",
                    "переписка со звездами", "/date",
                    "задать вопрос чату GPT", "/gpt");
            return;
        }


        // GPT
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");
            String answer = chatGPTService.sendMessage(prompt, message);
            sendChatAction(ActionType.TYPING);
            sendTextMessage(answer);
            return;
        }


        // DATE
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextMessage(text);
            sendTextButtonsMessage("Выберите девушку/парня, которую хотите пригласить на свидание.",
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райан Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Твоя задача пригласить звезду на свидание за 5 сообщений");

                String prompt = loadPrompt(query);
                chatGPTService.setPrompt(prompt);
                return;
            }

            sendChatAction(ActionType.TYPING);
            String answer = chatGPTService.addMessage(message);
            sendTextMessage(answer);
            return;
        }


        // MESSAGE
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            String text = loadMessage("message");
            sendTextMessage(text);

            sendTextButtonsMessage("Пришлите вашу переписку",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                sendChatAction(ActionType.TYPING);
                String answer = chatGPTService.sendMessage(prompt, userChatHistory);
                sendTextMessage(answer);
            }

            list.add(message);
            return;
        }


        // PROFILE
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo();
            questionCount = 1;
            String text = loadMessage("profile");
            sendTextMessage(text);
            sendTextMessage("Сколько вам лет?");
            return;
        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    me.age = message;
                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount = 3;
                    sendTextMessage("Есть ли у вас хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам не нравится в людях?");
                    return;
                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    me.goals = message;

                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");

                    sendChatAction(ActionType.TYPING);
                    String answer = chatGPTService.sendMessage(prompt, aboutMyself);
                    sendTextMessage(answer);
                    return;
            }
            return;
        }


        // OPENER
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            her = new UserInfo();
            questionCount = 1;

            String text = loadMessage("opener");
            sendTextMessage(text);
            sendTextMessage("Имя девушки");
            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    her.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    her.age = message;
                    questionCount = 3;
                    sendTextMessage("Кем она работает?");
                    return;
                case 3:
                    her.occupation = message;
                    questionCount = 4;
                    sendTextMessage("Есть ли у нее цели?");
                    return;
                case 4:
                    her.goals = message;
                    questionCount = 5;
                    sendTextMessage("Есть ли у нее хобби?");
                    return;
                case 5:
                    her.hobby = message;
                    String prompt = loadPrompt("opener");

                    sendChatAction(ActionType.TYPING);
                    String answer = chatGPTService.sendMessage(prompt, message);
                    sendTextMessage(answer);
                    return;
            }
            return;
        }

        sendTextMessage("Привет! Это бот \n\nВыберите режим работы через меню внизу слева");

    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
