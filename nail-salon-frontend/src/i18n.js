import { createI18n } from 'vue-i18n';

const messages = {
  zh: {
    nav: {
      book: '欢迎预约',
      menu: '选择服务项目',
      selectTime: '选择时间',
      fillInfo: '填写信息',
      success: '预约成功'
    },
    home: {
      subtitle: '请选择您的身份',
      newCustomer: '新客首次体验 (优惠价)',
      oldCustomer: '老客常规预约',
      location: '📍 饭山满站步行 5 分钟',
      showGuide: '查看路线指引图集',
      langToggle: '日本語',
      portfolio: '美甲作品集',
      showPortfolio: '浏览精选作品',
      loginName: '怎么称呼您？',
      loginNamePlaceholder: '请输入您的姓名',
      loginContact: '联系方式',
      loginContactPlaceholder: '请输入微信号或手机号',
      loginBtn: '开始预约',
      welcomeNew: '欢迎您，首次体验！',
      welcomeOld: '欢迎回来！',
      startBooking: '开始预约',
      firstVisit: '首次体验预约',
      switchUser: '切换账号'
    },
    menu: {
      next: '我已经看好了，去选时间'
    },
    date: {
      availableSlots: '可用时段',
      loading: '加载中...',
      available: '可选',
      full: '已满',
      next: '下一步'
    },
    form: {
      summaryTitle: '预约详情',
      date: '日期',
      time: '时间',
      identity: '身份',
      nameLabel: '姓名',
      namePlaceholder: '请输入怎么称呼您',
      contactLabel: '联系方式',
      contactPlaceholder: '请输入微信号或手机号',
      removalLabel: '卸甲类型',
      removalType: {
        natural: '本甲',
        extension: '甲片',
        none: '无'
      },
      remarksLabel: '备注',
      remarksPlaceholder: '有什么特别的要求吗？(选填)',
      imageLabel: '款式参考图(选填)',
      submitBtn: '确认预约',
      uploading: '上传中...',
      uploadFailed: '上传失败',
      submitFailed: '预约失败',
      networkError: '网络异常，请重试'
    },
    success: {
      title: '预约提交成功！',
      desc: '请完成定金支付以确认预约。取消预约定金恕不退还。',
      qrTip: '请扫码添加客服微信确认款式',
      qrAlt: '长按识别二维码或截图扫码',
      homeBtn: '返回首页'
    },
    booking: {
      myBookings: '我的预约',
      enterContact: '输入您的微信号或手机号',
      contactPlaceholder: '请输入预约时填写的联系方式',
      search: '查询',
      noRecords: '暂无预约记录',
      cancel: '取消预约',
      cancelConfirm: '预约取消后定金不予退还。如需更改时间请选择改期。如需退款请联系店主微信。',
      cancelSuccess: '预约已取消',
      cancelFailed: '取消失败',
      reschedule: '改期',
      rescheduleConfirm: '确定要更改预约时间吗？',
      rescheduleSuccess: '改期成功',
      rescheduleFailed: '改期失败',
      selectNewTime: '选择新时间',
      status: {
        PENDING_DEPOSIT: '待付定金',
        CONFIRMED: '预约已确认',
        BOOKED: '已预约',
        CANCELLED: '已取消',
        COMPLETED: '已完成'
      },
      deposit: {
        NONE: '未付定金',
        CUSTOMER_PAID: '已付款，等待确认',
        PAID: '定金已付',
        PENDING_REFUND: '退款处理中',
        REFUNDED: '定金已退',
        FORFEITED: '定金已没收'
      }
    }
  },
  ja: {
    nav: {
      book: 'ネイル予約',
      menu: 'メニュー選択',
      selectTime: '日時選択',
      fillInfo: '情報入力',
      success: '予約完了'
    },
    home: {
      subtitle: 'お客様の種類をお選びください',
      newCustomer: 'ご新規様 (初回特別価格)',
      oldCustomer: 'リピーター様',
      location: '📍 飯山満駅から徒歩5分',
      showGuide: '道順案内を見る',
      langToggle: '中文',
      portfolio: 'ネイルギャラリー',
      showPortfolio: '作品を見る',
      loginName: 'お名前は？',
      loginNamePlaceholder: 'お名前を入力してください',
      loginContact: 'ご連絡先',
      loginContactPlaceholder: 'WeChatまたは電話番号',
      loginBtn: '予約を始める',
      welcomeNew: 'はじめまして！',
      welcomeOld: 'おかえりなさい！',
      startBooking: '予約する',
      firstVisit: '初回予約',
      switchUser: 'アカウント切替'
    },
    menu: {
      next: 'メニューを決めたので日時を選ぶ'
    },
    date: {
      availableSlots: 'の空き状況',
      loading: '読み込み中...',
      available: '予約可',
      full: '満席',
      next: '次へ'
    },
    form: {
      summaryTitle: '予約内容',
      date: '日付',
      time: '時間',
      identity: 'お客様',
      nameLabel: 'お名前',
      namePlaceholder: 'お名前を入力してください',
      contactLabel: '連絡先',
      contactPlaceholder: 'WeChatまたは電話番号',
      removalLabel: 'オフ(除去)の種類',
      removalType: {
        natural: '自爪',
        extension: 'チップ/長さ出し',
        none: 'なし'
      },
      remarksLabel: 'ご要望',
      remarksPlaceholder: '特別なご要望があればご記入ください（任意）',
      imageLabel: '参考画像（任意）',
      submitBtn: '予約を確定する',
      uploading: 'アップロード中...',
      uploadFailed: '失敗しました',
      submitFailed: '予約に失敗しました',
      networkError: '通信エラーが発生しました'
    },
    success: {
      title: '予約を受け付けました！',
      desc: 'デポジットのお支払いが完了すると予約が確定します。キャンセル時のデポジットは返金されません。',
      qrTip: 'WeChatのQRコードをスキャンして追加してください',
      qrAlt: 'QRコードを長押しするかスクショで読み取り',
      homeBtn: 'トップへ戻る'
    },
    booking: {
      myBookings: '予約確認',
      enterContact: 'ご連絡先を入力',
      contactPlaceholder: '予約時にご登録いただいた連絡先',
      search: '検索',
      noRecords: '予約履歴がありません',
      cancel: '予約をキャンセル',
      cancelConfirm: 'キャンセル後のデポジットは返金されません。日時変更をご希望の場合は「日程変更」をお選びください。返金をご希望の場合は店主のWeChatまでご連絡ください。',
      cancelSuccess: '予約をキャンセルしました',
      cancelFailed: 'キャンセルに失敗しました',
      reschedule: '日程変更',
      rescheduleConfirm: '予約日時を変更してもよろしいですか？',
      rescheduleSuccess: '日程変更が完了しました',
      rescheduleFailed: '日程変更に失敗しました',
      selectNewTime: '新しい日時を選択',
      status: {
        PENDING_DEPOSIT: 'デポジット未払い',
        CONFIRMED: '予約確定',
        BOOKED: '予約済み',
        CANCELLED: 'キャンセル済み',
        COMPLETED: '完了'
      },
      deposit: {
        NONE: '未払い',
        CUSTOMER_PAID: '支払済み、確認待ち',
        PAID: 'デポジット支払済',
        PENDING_REFUND: '返金処理中',
        REFUNDED: '返金済み',
        FORFEITED: '没収'
      }
    }
  }
};

const i18n = createI18n({
  legacy: false,
  locale: 'zh',
  fallbackLocale: 'zh',
  messages,
});

export default i18n;
