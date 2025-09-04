

import React, { useEffect, useState } from "react";
import axios from "axios";
import UserForm from "../components/UserForm";
import ConfirmDialog from "../components/ConfirmDialog";
import type { User } from "../components/UserForm";
import { useWebSocket } from "../ws/WebSocketProvider";
import { useNavigate } from "react-router-dom";
import type { IMessage } from "@stomp/stompjs";

const Management: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [editingUser, setEditingUser] = useState<User | undefined>(undefined);
  const [showForm, setShowForm] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<User | undefined>(undefined);
  const stompClient = useWebSocket();
  const navigate = useNavigate();

  // Fetch users from backend
  useEffect(() => {
  axios.get<User[]>("http://localhost:8080/users")
      .then(res => {
        if (Array.isArray(res.data)) {
          setUsers(res.data);
        } else {
          setUsers([]);
        }
      })
      .catch(() => setUsers([]));
  }, []);

  useEffect(() => {
  console.log("stompClient", stompClient);
  if (!stompClient) return;

  console.log("🔔 Subscribing to /topic/manager/users");

  // Sửa lại cách subscribe
  const subscription = stompClient.subscribe(
    "/topic/manager/users", 
    (message: IMessage) => {

      try {
        // PARSE BODY - quan trọng!
        const data = JSON.parse(message.body);

        // Xử lý theo cấu trúc data của bạn
        if (data.action && data.user) {
          console.log("Action:", data.action, "User:", data.user);
          
          if (data.action === "add") {
            setUsers(prev => {
              if (prev.find(u => u.id === data.user.id)) return prev;
              return [data.user, ...prev];
            });
          } else if (data.action === "update") {
            setUsers(prev => prev.map(u => u.id === data.user.id ? data.user : u));
          } else if (data.action === "delete") {
            setUsers(prev => prev.filter(u => u.id !== data.user.id));
          }
        } else {
          console.warn("Invalid message format:", data);
        }
      } catch (error) {
        console.error("❌ Parse error:", error, "Raw body:", message.body);
      }
    },
    { // Additional headers if needed
      'id': 'sub-001',
      'persistent': 'true'
    }
  );

  console.log("✅ Subscription created:", subscription.id);

  return () => {
    console.log("🗑️ Unsubscribing");
    subscription.unsubscribe();
  };
}, [stompClient]);

  const handleAdd = () => {
    setEditingUser(undefined);
    setShowForm(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    setShowForm(true);
  };

  const handleDeleteClick = (user: User) => {
    setUserToDelete(user);
    setConfirmOpen(true);
  };

  const handleConfirmDelete = () => {
    if (userToDelete) {
      axios.delete(`http://localhost:8080/users/${userToDelete.id}`);
    }
    setConfirmOpen(false);
    setUserToDelete(undefined);
  };

  const handleCancelDelete = () => {
    setConfirmOpen(false);
    setUserToDelete(undefined);
  };

  const handleSave = (user: User) => {
    if (editingUser) {
      axios.put(`http://localhost:8080/users/${user.id}`, user);
    } else {
      axios.post("http://localhost:8080/users", user);
    }
    setShowForm(false);
  };

  const handleCancel = () => {
    setShowForm(false);
  };

  return (
    <div className="p-8 min-h-screen">
      <div className="max-w-3xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">Quản lý người dùng</h1>
          <button
            className="bg-blue-600 text-white px-4 py-2 rounded shadow hover:bg-blue-700 transition"
            onClick={handleAdd}
          >
            Thêm người dùng
          </button>
        </div>
        <div className="overflow-x-auto rounded-lg shadow">
          <table className="min-w-full bg-white">
            <thead className="bg-blue-100">
              <tr>
                <th className="py-3 px-4 text-left font-semibold text-gray-700">ID</th>
                <th className="py-3 px-4 text-left font-semibold text-gray-700">Tên</th>
                <th className="py-3 px-4 text-left font-semibold text-gray-700">Số điện thoại</th>
                <th className="py-3 px-4 text-left font-semibold text-gray-700">Hành động</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id} className="border-b hover:bg-blue-50">
                  <td className="py-2 px-4">{user.id}</td>
                  <td className="py-2 px-4">{user.name}</td>
                  <td className="py-2 px-4">{user.phone}</td>
                  <td className="py-2 px-4 flex gap-2">
                    <button
                      className="bg-purple-500 text-white px-3 py-1 rounded hover:bg-purple-600 transition"
                      onClick={() => handleEdit(user)}
                    >
                      Sửa
                    </button>
                    <button
                      className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600 transition"
                      onClick={() => handleDeleteClick(user)}
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {/* <button onClick={() => navigate("/test")}>Go to Test Page</button> */}
        </div>
      </div>
      {showForm && (
        <UserForm
          user={editingUser}
          onSave={handleSave}
          onCancel={handleCancel}
        />
      )}
      <ConfirmDialog
        open={confirmOpen}
        title="Xác nhận xoá người dùng"
        message={`Bạn có chắc muốn xoá người dùng "${userToDelete?.name}"?`}
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
      />
    </div>
  );
};

export default Management;