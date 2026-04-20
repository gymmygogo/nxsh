<template>
  <view class="page">
    <view class="page-header">
      <view class="header-left">
        <view class="page-mark"><text class="page-mark-t">联</text></view>
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
        <view class="empty-line" />
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
.page { min-height: 100vh; background: #f0f1f3; padding: 0 16px 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 0 16px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.page-mark {
  width: 44px; height: 44px; border-radius: 12px; background: #fff; border: 1px solid #e6e8ec;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.page-mark-t { font-size: 17px; font-weight: 700; color: #1e4a72; }
.page-title { font-size: 20px; font-weight: 600; color: #1a1d21; display: block; }
.page-sub { font-size: 12px; color: #6b7280; display: block; margin-top: 2px; }
.btn-add {
  background: #1e4a72; color: #fff;
  font-size: 13px; font-weight: 600; margin: 0; padding: 0 16px;
  height: 34px; line-height: 34px; border-radius: 10px; border: 1px solid #1e4a72;
}
.contact-list { display: flex; flex-direction: column; gap: 10px; }
.contact-card {
  background: #ffffff; border-radius: 12px; padding: 16px;
  display: flex; align-items: center; gap: 12px;
  border: 1px solid #e6e8ec;
}
.contact-avatar {
  width: 44px; height: 44px; border-radius: 12px;
  background: #e8ecf2;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  border: 1px solid #e6e8ec;
}
.avatar-text { font-size: 18px; font-weight: 700; color: #1e4a72; }
.info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.c-name { font-size: 16px; font-weight: 600; color: #1a1d21; }
.c-detail { font-size: 13px; color: #6b7280; }
.c-priority { font-size: 11px; color: #9ca3af; }
.actions { display: flex; gap: 6px; flex-shrink: 0; }
.btn-icon { width: 32px; height: 32px; line-height: 32px; padding: 0; font-size: 14px; border-radius: 8px; border: 1px solid #e6e8ec; }
.up { background: #f4f6f9; color: #1e4a72; }
.down { background: #f4f6f9; color: #1e4a72; }
.del { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }
.empty { text-align: center; padding: 40px 0; }
.empty-line {
  width: 28px; height: 2px; background: #d1d5db; border-radius: 1px;
  margin: 0 auto 10px;
}
.empty-text { font-size: 14px; color: #9ca3af; }
.modal {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 1000;
  display: flex; justify-content: center; align-items: center;
}
.modal-box { background: #fff; width: 85%; border-radius: 14px; padding: 24px; border: 1px solid #e6e8ec; }
.modal-title { font-size: 17px; font-weight: 600; color: #1a1d21; display: block; margin-bottom: 18px; text-align: center; }
.modal-field { margin-bottom: 14px; }
.modal-label { font-size: 13px; font-weight: 600; color: #6b7280; display: block; margin-bottom: 6px; }
.modal-input {
  height: 44px; border: 1px solid #e6e8ec; border-radius: 10px;
  padding: 0 14px; font-size: 15px; background: #ffffff; color: #1a1d21;
}
.modal-btns { display: flex; gap: 12px; margin-top: 6px; }
.btn-cancel {
  flex: 1; background: #f3f4f6; color: #4b5563; height: 44px; line-height: 44px;
  font-size: 15px; font-weight: 600; border-radius: 10px; border: 1px solid #e5e7eb;
}
.btn-confirm {
  flex: 1; background: #1e4a72; color: #fff;
  height: 44px; line-height: 44px; font-size: 15px; font-weight: 600; border-radius: 10px; border: 1px solid #1e4a72;
}
</style>
