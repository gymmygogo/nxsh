<template>
  <view class="page">
    <view class="page-header">
      <view class="header-left">
        <text class="page-icon">📞</text>
        <view>
          <text class="page-title">紧急联系人</text>
          <text class="page-sub">管理紧急呼叫对象</text>
        </view>
      </view>
      <button class="btn-add" @click="openAdd">+ 添加</button>
    </view>

    <view class="contact-list">
      <view class="contact-card" v-for="(contact, index) in contacts" :key="contact.id">
        <view class="contact-avatar">
          <text class="avatar-text">{{ contact.name?.charAt(0) || '?' }}</text>
        </view>
        <view class="info">
          <text class="c-name">{{ contact.name }}</text>
          <text class="c-detail">{{ contact.relationship }} · {{ contact.phone }}</text>
          <text class="c-priority">优先级 {{ contact.priority }}</text>
        </view>
        <view class="actions">
          <button class="btn-icon up" @click="moveUp(index)" :disabled="index === 0">▲</button>
          <button class="btn-icon down" @click="moveDown(index)" :disabled="index === contacts.length - 1">▼</button>
          <button class="btn-icon del" @click="remove(contact.id)">✕</button>
        </view>
      </view>
      <view v-if="contacts.length === 0" class="empty">
        <text class="empty-icon">📇</text>
        <text class="empty-text">暂无紧急联系人</text>
      </view>
    </view>

    <view class="modal" v-if="showModal">
      <view class="modal-box">
        <text class="modal-title">添加联系人</text>
        <view class="modal-field">
          <text class="modal-label">姓名</text>
          <input class="modal-input" v-model="form.name" placeholder="请输入姓名" />
        </view>
        <view class="modal-field">
          <text class="modal-label">手机号</text>
          <input class="modal-input" v-model="form.phone" type="number" placeholder="请输入手机号" />
        </view>
        <view class="modal-field">
          <text class="modal-label">关系</text>
          <input class="modal-input" v-model="form.relationship" placeholder="如：儿子" />
        </view>
        <view class="modal-btns">
          <button class="btn-cancel" @click="showModal = false">取消</button>
          <button class="btn-confirm" @click="handleSave">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/request.js'

const contacts = ref([])
const showModal = ref(false)
const form = ref({ name: '', phone: '', relationship: '', priority: 1 })

const getFamilyId = () => uni.getStorageSync('familyId')
const getElderlyId = () => uni.getStorageSync('elderlyId')

const loadContacts = () => {
  request({
    url: '/family/emergency-contact/list',
    method: 'GET',
    data: { familyId: getFamilyId(), elderlyId: getElderlyId() },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        contacts.value = res.data.data.sort((a, b) => a.priority - b.priority)
      }
    }
  })
}

onMounted(() => {
  loadContacts()
})

const openAdd = () => {
  form.value = { name: '', phone: '', relationship: '', priority: contacts.value.length + 1 }
  showModal.value = true
}

const handleSave = () => {
  if (!form.value.name || !form.value.phone) return uni.showToast({title: '请填写完整', icon: 'none'})
  request({
    url: '/family/emergency-contact',
    method: 'POST',
    data: {
      familyId: getFamilyId(),
      elderlyId: getElderlyId(),
      ...form.value
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        uni.showToast({ title: '添加成功' })
        showModal.value = false
        loadContacts()
      } else {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    }
  })
}

const updateContact = (contact) => {
  return new Promise((resolve) => {
    request({
      url: `/family/emergency-contact/${contact.id}`,
      method: 'PUT',
      data: {
        familyId: getFamilyId(),
        elderlyId: getElderlyId(),
        name: contact.name,
        phone: contact.phone,
        relationship: contact.relationship,
        priority: contact.priority
      },
      success: resolve
    })
  })
}

const moveUp = async (index) => {
  if (index === 0) return
  const current = contacts.value[index]
  const prev = contacts.value[index - 1]

  // Swap priorities
  const tempPrio = current.priority
  current.priority = prev.priority
  prev.priority = tempPrio

  uni.showLoading({title: '调整中'})
  await updateContact(current)
  await updateContact(prev)
  uni.hideLoading()
  loadContacts()
}

const moveDown = async (index) => {
  if (index === contacts.value.length - 1) return
  const current = contacts.value[index]
  const next = contacts.value[index + 1]

  // Swap priorities
  const tempPrio = current.priority
  current.priority = next.priority
  next.priority = tempPrio

  uni.showLoading({title: '调整中'})
  await updateContact(current)
  await updateContact(next)
  uni.hideLoading()
  loadContacts()
}

const remove = (id) => {
  uni.showModal({
    title: '确认删除',
    success: (res) => {
       if(res.confirm){
         request({
          url: `/family/emergency-contact/${id}`,
          method: 'DELETE',
          data: { familyId: getFamilyId() },
          success: (delRes) => {
            if (delRes.statusCode === 200 && delRes.data.code === 200) loadContacts()
          }
         })
       }
    }
  })
}
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #EDF4FF 0%, #F8FAFF 30%); padding: 0 16px 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 0 16px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.page-icon { font-size: 28px; }
.page-title { font-size: 20px; font-weight: 800; color: #2D2D2D; display: block; }
.page-sub { font-size: 12px; color: #7A9BBF; display: block; margin-top: 2px; }
.btn-add {
  background: linear-gradient(135deg, #6DB3F2, #5B9BD5); color: #fff;
  font-size: 13px; font-weight: 600; margin: 0; padding: 0 16px;
  height: 34px; line-height: 34px; border-radius: 17px; border: none;
}
.contact-list { display: flex; flex-direction: column; gap: 12px; }
.contact-card {
  background: #FFFFFF; border-radius: 16px; padding: 16px;
  display: flex; align-items: center; gap: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.contact-avatar {
  width: 44px; height: 44px; border-radius: 14px;
  background: linear-gradient(135deg, #D6EBFF, #BDDCFF);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.avatar-text { font-size: 18px; font-weight: 800; color: #5B9BD5; }
.info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.c-name { font-size: 16px; font-weight: 700; color: #2D2D2D; }
.c-detail { font-size: 13px; color: #7A9BBF; }
.c-priority { font-size: 11px; color: #A0B8CF; }
.actions { display: flex; gap: 6px; flex-shrink: 0; }
.btn-icon { width: 32px; height: 32px; line-height: 32px; padding: 0; font-size: 14px; border-radius: 8px; border: none; }
.up { background: #EDF4FF; color: #5B9BD5; }
.down { background: #EDF4FF; color: #5B9BD5; }
.del { background: #FFF1F0; color: #FF4D4F; }
.empty { text-align: center; padding: 40px 0; }
.empty-icon { font-size: 36px; display: block; margin-bottom: 8px; }
.empty-text { font-size: 14px; color: #A0B8CF; }
.modal {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 1000;
  display: flex; justify-content: center; align-items: center;
}
.modal-box { background: #fff; width: 85%; border-radius: 20px; padding: 24px; }
.modal-title { font-size: 18px; font-weight: 700; color: #2D2D2D; display: block; margin-bottom: 18px; text-align: center; }
.modal-field { margin-bottom: 14px; }
.modal-label { font-size: 13px; font-weight: 600; color: #6B8CAA; display: block; margin-bottom: 6px; }
.modal-input {
  height: 44px; border: 1px solid #DEE9F5; border-radius: 12px;
  padding: 0 14px; font-size: 15px; background: #F5F9FF; color: #2D2D2D;
}
.modal-btns { display: flex; gap: 12px; margin-top: 6px; }
.btn-cancel {
  flex: 1; background: #F0F5FC; color: #7A9BBF; height: 44px; line-height: 44px;
  font-size: 15px; font-weight: 600; border-radius: 12px; border: none;
}
.btn-confirm {
  flex: 1; background: linear-gradient(135deg, #6DB3F2, #5B9BD5); color: #fff;
  height: 44px; line-height: 44px; font-size: 15px; font-weight: 600; border-radius: 12px; border: none;
}
</style>
